package dhm

object Expr {

  type name = String

  sealed trait Expr
  case class Var(a:name) extends Expr // variable
  case class Call(a:Expr,b:List[Expr]) extends Expr // application
  case class Fun(a:List[name], b:Expr) extends Expr // abstraction
  case class Let(a:name, b:Expr, c: Expr) extends Expr // let
  case class RecordSelect(a:Expr, b: name) extends Expr          // selecting value of label: `r.a`
  case class RecordExtend(a:name, b: Expr, c: Expr) extends Expr // extending a record: `{a = 1 | r}`
  case class RecordRestrict(a: Expr, b: name) extends Expr       // deleting a label: `{r - a}`
  case object RecordEmpty extends Expr                           // empty record: `{}`

  case class Ref[A](var a:A)

  type id = Int
  type level = Int

  sealed trait Ty
  case class TConst(a: name) extends Ty // type constant: `int` or `bool`
  case class TApp(a: Ty, b:List[Ty]) extends Ty // type application: `list[int]`
  case class TArrow(a: List[Ty], b: Ty) extends Ty // function type: `(int, int) -> int`
  case class TVar(a: Ref[TVal]) extends Ty // type variable
  case class TRecord(a:Row) extends Ty                  // record type: `{<...>}`
  case object TRowEmpty extends Ty                      // empty row: `<>`
  case class TRowExtend(a:name, b:Ty, c:Row) extends Ty // row extension: `<a : _ | ...>`
  type Row = Ty // the kind of rows - empty row, row variable, or row extension




  sealed trait TVal
  case class Unbound(a: id, b: level) extends TVal
  case class Link(a: Ty) extends TVal
  case class Generic(a: id) extends TVal

  def string_of_expr(expr: Expr): String = {
    def f(is_simple: Boolean, expr: Expr): String = {
      expr match {
      case Var(name) => name
      case Call(fn_expr, arg_list) =>
        f(true, fn_expr) + "(" + arg_list.map(f(false,_)).mkString(", ") + ")"
      case Fun(param_list, body_expr) =>
        val fun_str = "fun " + param_list.mkString(" ") + " -> " + f(false, body_expr)
        if (is_simple) "(" + fun_str + ")" else fun_str
      case Let(var_name, value_expr, body_expr) =>
        val let_str = "let " + var_name + " = " + f(false, value_expr) + " in " + f(false, body_expr)
        if (is_simple) "(" + let_str + ")" else let_str

      case RecordEmpty => "{}"
      case RecordSelect(record_expr, label) => f(true, record_expr) + "." + label
      case RecordRestrict(record_expr, label) => "{" + f(false, record_expr) + " - " + label + "}"
      case RecordExtend(label, expr, record_expr) =>
          def g(str:String, e:Expr):String = {
            e match {
            case RecordEmpty => str
            case RecordExtend(label, expr, record_expr) =>
              g(str + ", " + label + " = " + f(false, expr), record_expr)
            case other_expr => str + " | " + f(false, other_expr)
            }
          }
          "{" + g(label + " = " + f(false, expr), record_expr) + "}"
      }
    }
    f(false, expr)
  }

  def string_of_ty(ty: Ty): String = {
    var id_name_map:Map[id, String] = Map()
    val count = Ref(0)
    def next_name() = {
      val i = count.a
      count.a += 1
      (97 + i % 26).toChar.toString + (if (i >= 26) ""+(i / 26) else "")
    }
    def f(is_simple: Boolean, ty: Ty): String = {
      ty match {
        case TConst(name) => name
        case TApp(ty, ty_arg_list) =>
          f(true, ty) + "[" + ty_arg_list.map(f(false, _)).mkString(", ") + "]"
        case TArrow(param_ty_list, return_ty) =>
          val arrow_ty_str =
            param_ty_list match {
              case List(param_ty) =>
                val param_ty_str = f(true, param_ty)
                val return_ty_str = f(false, return_ty)
                param_ty_str + " -> " + return_ty_str
              case _ =>
                val param_ty_list_str = param_ty_list.map(f(false, _)).mkString(", ")
                val return_ty_str = f(false, return_ty)
                "(" + param_ty_list_str + ") -> " + return_ty_str
            }
          if (is_simple) "(" + arrow_ty_str + ")" else arrow_ty_str
        case TVar(Ref(Generic(id))) =>
          try {
            id_name_map(id)
          } catch {
            case _: Throwable =>
              val name = next_name()
              id_name_map = id_name_map + (id -> name)
              name
          }
        case TVar(Ref(Unbound(id, _))) => "_" + id
        case TVar(Ref(Link(ty))) => f(is_simple, ty)
        case TRecord(row_ty) => "{" + f(false, row_ty) + "}"
        case TRowEmpty => ""
        case TRowExtend(label, ty, row_ty) =>
          def g(str: String, t:Ty): String = {
            t match {
              case TRowEmpty => str
              case TRowExtend(label, ty, row_ty) =>
                g(str + ", " + label + " : " + f(false, ty), row_ty)
              case TVar(Ref(Link(ty))) => g(str, ty)
              case other_ty => str + " | " + f(false, other_ty)
            }
          }
          g(label + " : " + f(false, ty), row_ty)
      }
    }
    val ty_str = f(false, ty)
    if (count.a > 0) {
      val var_names = id_name_map.toList.foldLeft(List[String]()){
        case (acc, (_, value)) => value :: acc
      }
      "forall[" + var_names.sorted.mkString(" ") + "] " + ty_str
    } else {
      ty_str
    }
  }

}
