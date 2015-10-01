package fcp

object Expr {

  type name = String
  type id = Int
  type level = Int

  case class Ref[A](var a:A)

  sealed trait ty
  case class TConst(a:name) extends ty                    // type constant: `int` or `bool`
  case class TApp(a:ty, b: List[ty]) extends ty              // type application: `list[int]`
  case class TArrow(a:List[ty], b:ty) extends ty            // function type: `(int, int) -> int`
  case class TVar(a:Ref[tval]) extends ty                 // type variable
  case class TForall(a:List[id], b:ty) extends ty          // polymorphic type: `forall[a] a -> a`

  sealed trait tval
  case class Unbound(a: id, b: level) extends tval
  case class Link(a: ty) extends tval
  case class Generic(a: id) extends tval
  case class Bound(a: id) extends tval

  def unlink(t:ty):ty = {
    t match {
    case TVar(tvar@Ref(Link(ty))) =>
        val ty1 = unlink(ty)
        tvar.a = Link(ty1)
        ty1
    case ty => ty
    }
  }

  def is_monomorphic(t:ty):Boolean = {
    t match {
    case TForall(_,_) => false
    case TConst(_) => true
    case TVar(Ref(Link(ty))) => is_monomorphic(ty)
    case TVar(_) => true
    case TApp(ty, ty_arg_list) =>
        is_monomorphic(ty) && ty_arg_list.forall(is_monomorphic)
    case TArrow(param_ty_list, return_ty) =>
        param_ty_list.forall(is_monomorphic) && is_monomorphic(return_ty)
    }
  }

  type ty_ann = (List[id], ty)             // type annotation: `some[a b] a -> b`

  sealed trait expr
  case class Var(a: name) extends expr                           // variable
  case class Call(a: expr, b:List[expr]) extends expr              // application
  case class Fun(a: List[(name, Option[ty_ann])], b: expr) extends expr    // abstraction
  case class Let(a: name, b: expr, c: expr) extends expr             // let
  case class Ann(a: expr, b: ty_ann) extends expr                  // type annotation: `1 : int`

  def is_annotated(e:expr):Boolean = {
    e match {
    case Ann(_, _) => true
    case Let(_, _, body) => is_annotated(body)
    case _ => false
    }
  }

  // module IntMap = Map.Make (struct type t = int let compare = compare end)

  def name_of_int(i:Int):String = {
    val name = (97 + i % 26).toChar.toString
    if (i >= 26) name+(i / 26) else name
  }

  def extend_name_map(name_map:Map[id,String], var_id_list:List[id]) = {
    val (name_list_rev, name_map1) =
      var_id_list.foldLeft((List[String](), name_map)) {
        case ((name_list, name_map), var_id) =>
          val new_name = name_of_int(name_map.size)
          (new_name :: name_list, name_map + (var_id -> new_name))
      }
    (name_list_rev.reverse, name_map1)
  }

  def string_of_ty_with_bound_tvars(name_map:Map[id, String], ty:ty):String = {
    def complex(name_map:Map[id, String], t:ty):String = {
      t match {
      case TArrow(param_ty_list, return_ty) =>
          val param_ty_list_str = param_ty_list match {
            case List(param_ty) => simple(name_map, param_ty)
            case _ => "(" + param_ty_list.map(complex(name_map, _)).mkString(", ") + ")"
          }
          val return_ty_str = complex(name_map, return_ty)
          param_ty_list_str + " -> " + return_ty_str
      case TForall(var_id_list, ty) =>
          val (name_list, name_map1) = extend_name_map(name_map, var_id_list)
          val name_list_str = name_list.mkString(" ")
          "forall[" + name_list_str + "] " + complex(name_map1, ty)
      case TVar(Ref(Link(ty))) => complex(name_map, ty)
      case ty => simple(name_map, ty)
      }
    }
    def simple(name_map:Map[id, String], t:ty):String = {
      t match {
      case TConst(name) => name
      case TApp(ty, ty_arg_list) =>
          val ty_str = simple(name_map, ty)
          val ty_arg_list_str = ty_arg_list.map(complex(name_map,_)).mkString(", ")
          ty_str + "[" + ty_arg_list_str + "]"
      case TVar(Ref(Unbound(id, _))) => "@unknown" + id
      case TVar(Ref(Bound(id))) => name_map(id)
      case TVar(Ref(Generic(id))) => "@generic" + id
      case TVar(Ref(Link(ty))) => simple(name_map, ty)
      case ty => "(" + complex(name_map, ty) + ")"
      }
    }
    complex(name_map, ty)
  }

  def string_of_ty(ty:ty) : String = {
    string_of_ty_with_bound_tvars(Map[id,String](), ty)
  }

  def string_of_ty_ann(var_id_list:List[id], ty:ty) : String = {
    val (name_list, name_map) = extend_name_map(Map[id,String](), var_id_list)
    val ty_str = string_of_ty_with_bound_tvars(name_map, ty)
    name_list match {
      case List() => ty_str
      case _ => "some[" + name_list.mkString(" ") + "] " + ty_str
    }
  }

  def string_of_expr(expr:expr) : String = {
    def complex(e:expr):String = {
      e match {
      case Fun(param_list, body_expr) =>
          val param_list_str =
              param_list.map{ case (param_name, maybe_ty_ann) =>
                maybe_ty_ann match {
                  case Some((ty,ann)) => "(" + param_name + " : " + string_of_ty_ann(ty,ann) + ")"
                  case None => param_name
                }
              }.mkString(" ")
          "fun " + param_list_str + " -> " + complex(body_expr)
      case Let(var_name, value_expr, body_expr) =>
          "let " + var_name + " = " + complex(value_expr) + " in " + complex(body_expr)
      case Ann(expr, (ty,ann)) =>
          simple(expr) + " : " + string_of_ty_ann(ty, ann)
      case expr => simple(expr)
      }
    }
    def simple(e:expr):String = {
      e match {
      case Var(name) => name
      case Call(fn_expr, arg_list) =>
          simple(fn_expr) + "(" + arg_list.map(complex).mkString(", ") + ")"
      case expr => "(" + complex(expr) + ")"
      }
    }
    complex(expr)
  }
}