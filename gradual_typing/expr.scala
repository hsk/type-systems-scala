package dhm

object Expr {

  type name = String

  sealed trait Expr
  case class Var(a:name) extends Expr // variable
  case class Call(a:Expr,b:List[Expr]) extends Expr // application
  case class Fun(a:List[(name, Option[Ty])], b:Expr) extends Expr // abstraction
  case class Let(a:name, t: Option[Ty], b:Expr, c: Expr) extends Expr // let
  case class Ann(a:Expr, b:Ty) extends Expr // type annotation

  case class Ref[A](var a:A)

  type id = Int
  type level = Int

  sealed trait Ty
  case class TConst(a: name) extends Ty // type constant: `int` or `bool`
  case class TApp(a: Ty, b:List[Ty]) extends Ty // type application: `list[int]`
  case class TArrow(a: List[Ty], b: Ty) extends Ty // function type: `(int, int) -> int`

  case class TVar(a: Ref[TVal]) extends Ty // type variable
  case object TDynamic extends Ty // dynamic type: `?`

  sealed trait TVal
  case class Unbound(a: id, b: level, c: Boolean) extends TVal
  case class Link(a: Ty) extends TVal
  case class Generic(a: id) extends TVal

  def string_of_ty_with_var_names(ty:Ty) = {
    var id_name_map:Map[id, String] = Map()
    val count = Ref(0)
    def next_name() = {
      val i = count.a
      count.a += 1
      (97 + i % 26).toChar.toString + (if (i >= 26) ""+(i / 26) else "")
    }

    def complex(ty: Ty): String = {
      ty match {
      case TArrow(param_ty_list, return_ty) =>
          val param_ty_list_str = param_ty_list match{
            case List(param_ty) => simple(param_ty)
            case _ => "(" + param_ty_list.map(complex).mkString(", ") + ")"
          }
          val return_ty_str = complex(return_ty)
          param_ty_list_str + " => " + return_ty_str
      case TVar(Ref(Link(ty))) => complex(ty)
      case ty => simple(ty)
      }
    }
    def simple(ty: Ty): String = {
      ty match {
      case TDynamic => "?"
      case TConst(name) => name
      case TApp(ty, ty_arg_list) =>
          simple(ty) + "[" + ty_arg_list.map(complex).mkString(", ") + "]"
      case TVar(Ref(Generic(id))) =>
            id_name_map.getOrElse(id,{
              val name = next_name()
              id_name_map = id_name_map + (id -> name)
              name
            })
        
      case TVar(Ref(Unbound(id, _, is_dynamic))) =>
          (if (is_dynamic) "@dynamic" else "@unknown") + id
      case TVar(Ref(Link(ty))) => simple(ty)
      case ty => "(" + complex(ty) + ")"
      }
    }
    val ty_str = complex(ty)
    if (count.a > 0) {
      val var_names = id_name_map.toList.foldLeft(List[String]()){
        case (acc, (_, value)) => value :: acc
      }
      (var_names.sorted, ty_str)
    } else {
      (List(), ty_str)
    }
  }

  def string_of_ty(ty:Ty) : String = {
    val (var_names, ty_str) = string_of_ty_with_var_names(ty)
    if (var_names.length > 0)
      "forall[" + var_names.mkString(" ") + "] " + ty_str
    else
      ty_str
  }

  def string_of_ty_ann(ty:Ty) : String = {
    val (var_names, ty_str) = string_of_ty_with_var_names(ty)
    if (var_names.length > 0)
      "some[" + var_names.mkString(" ") + "] " + ty_str
    else
      ty_str
  }

  def string_of_expr(expr:Expr) : String = {
    def complex(expr:Expr):String = {
      expr match {
      case Fun(param_list, body_expr) =>
          val param_list_str =
              param_list.map {
                case (param_name, maybe_ty_ann) =>
                  maybe_ty_ann match {
                    case Some(ty_ann) => "(" + param_name + " : " + string_of_ty_ann(ty_ann) + ")"
                    case None => param_name
                  }
              }.mkString(" ")
          "fun " + param_list_str + " => " + complex(body_expr)
      case Let(var_name, Some(ty_ann), value_expr, body_expr) =>
          "let " + var_name + " : " + string_of_ty_ann(ty_ann) +
            " = " + complex(value_expr) + " in " + complex(body_expr)
      case Let(var_name, None, value_expr, body_expr) =>
          "let " + var_name + " = " + complex(value_expr) + " in " + complex(body_expr)
      case Ann(expr, ty_ann) =>
          simple(expr) + " : " + string_of_ty_ann(ty_ann)
      case expr => simple(expr)
      }
    }
    def simple(expr:Expr):String = {
      expr match {
      case Var(name) => name
      case Call(fn_expr, arg_list) =>
          simple(fn_expr) + "(" + arg_list.map(complex).mkString(", ") + ")"
      case expr => "(" + complex(expr) + ")"
      }
    }
    complex(expr)
  }
}
