package refined

object Expr {
    
  type name = String
  type id = Int
  type level = Int

  // Types
  sealed trait Ty[A]
  case class TConst[A](a:name) extends Ty[A]
  case class TApp[A](a:name, b:List[Ty[A]]) extends Ty[A]
  case class TArrow[A](a:List[refined_ty[A]], b:refined_ty[A]) extends Ty[A]
  case class TVar[A](var a:TVal[A]) extends Ty[A]

  sealed trait refined_ty[A]
  case class Plain[A](a:Ty[A]) extends refined_ty[A]
  case class Named[A](a:name, b:Ty[A]) extends refined_ty[A]
  case class Refined[A](a: name, b:Ty[A], c:A) extends refined_ty[A]

  sealed trait TVal[A]
  case class Unbound[A](a:id, b: level) extends TVal[A]
  case class Link[A](a: Ty[A]) extends TVal[A]
  case class Generic[A](a: id) extends TVal[A]

  val t_bool = TConst[t_expr]("bool")

  def real_ty[A](t:Ty[A]):Ty[A] = {
    t match {
    case TVar(Link(ty)) => real_ty(ty)
    case ty => ty
    }
  }

  def plain_ty[A](rt: refined_ty[A]):Ty[A] = {
    rt match {
    case Plain(ty) => ty
    case Named(_, ty) => ty
    case Refined(_, ty, _) => ty
    }
  }

  def r_ty_map[A](f: Ty[A] => Ty[A], rt: refined_ty[A]): refined_ty[A] = {
    rt match {
      case Plain(ty) => Plain(f(ty))
      case Named(name, ty) => Named(name, f(ty))
      case Refined(name, ty, expr) => Refined(name, f(ty), expr)
    }
  }

  def is_function_ty[A](t:Ty[A]):Boolean = {
    t match {
      case TArrow(_, _) => true
      case TVar(Link(ty)) => is_function_ty(ty)
      case _ => false
    }
  }

  def strip_refined_types[A](ty:Ty[A]):Ty[A] = {
    ty match {
      // Removes refined types, while preserving type variable identity.
      case TApp(name, arg_ty_list) => TApp(name, arg_ty_list.map(strip_refined_types))
      case TArrow(param_r_ty_list, return_r_ty) =>
        def f[A](r_ty: refined_ty[A]):refined_ty[A] = Plain(strip_refined_types(plain_ty(r_ty)))
        TArrow(param_r_ty_list.map(f), f(return_r_ty))
      case TVar(Link(ty)) => strip_refined_types(ty)
      case TConst(_) => ty
      case TVar(_) => ty
    }
  }

  def duplicate_without_refined_types[A](ty:Ty[A]):Ty[A] = {
    ty match {
      // Removes refined types, duplicating the type (including the type variables).
      case TConst(name) => TConst(name)
      case TApp(name, arg_ty_list) =>
          TApp(name, arg_ty_list.map(duplicate_without_refined_types) )
      case TArrow(param_r_ty_list, return_r_ty) =>
          def f[A](r_ty:refined_ty[A]):refined_ty[A] = Plain(duplicate_without_refined_types(plain_ty(r_ty)))
          TArrow(param_r_ty_list.map(f), f(return_r_ty))
      case TVar(Link(ty)) => duplicate_without_refined_types(ty)
      case TVar(Unbound(id, level)) => TVar(Unbound(id, level))
      case TVar(Generic(id)) => TVar(Generic(id))
    }
  }

  // Syntax tree
  type s_ty = Ty[s_expr]
  type s_refined_ty = refined_ty[s_expr]

  sealed trait s_expr
  case class SVar(a: name) extends s_expr
  case class SBool(a: Boolean) extends s_expr
  case class SInt(a: Int) extends s_expr
  case class SCall(a: s_expr, b: List[s_expr]) extends s_expr
  case class SFun(a: List[s_param], b: Option[s_refined_ty], c: s_expr) extends s_expr
  case class SLet(a: name, b: s_expr, c: s_expr) extends s_expr
  case class SIf(a: s_expr, b: s_expr, c: s_expr) extends s_expr
  case class SCast(a: s_expr, b: s_ty, c: Option[s_expr]) extends s_expr

  case class s_param(a:name, b: Option[(s_ty, Option[s_expr])])


  // Typed expressions
  type t_ty = Ty[t_expr]
  type t_refined_ty = refined_ty[t_expr]

  case class t_expr(shape : t_expr_shape, ty : t_ty)
  sealed trait t_expr_shape

  case class EVar(a: name) extends t_expr_shape
  case class EBool(a: Boolean) extends t_expr_shape
  case class EInt(a: Int) extends t_expr_shape
  case class ECall(a: t_expr, b: List[t_expr]) extends t_expr_shape
  case class EFun(a: List[t_param], b: Option[t_refined_ty], c: t_expr) extends t_expr_shape
  case class ELet(a: name, b: t_expr, c: t_expr) extends t_expr_shape
  case class EIf(a: t_expr, b: t_expr, c: t_expr) extends t_expr_shape
  case class ECast(a: t_expr, b: t_ty, c: Option[t_expr]) extends t_expr_shape

  case class t_param(a:name, b: t_ty, c: Option[t_expr])

}

