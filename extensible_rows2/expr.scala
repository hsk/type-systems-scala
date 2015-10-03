package dhm

object Expr {

  type name = String

  def compare_label(label1:name, label2:name):Int = {
    val compare_length = label1.length - label2.length
    if (compare_length == 0)
      label1.compareTo(label2)
    else compare_length
  }

  sealed trait Expr
  case class Var(a:name) extends Expr // variable
  case class Call(a:Expr,b:List[Expr]) extends Expr // application
  case class Fun(a:List[name], b:Expr) extends Expr // abstraction
  case class Let(a:name, b:Expr, c: Expr) extends Expr // let
  case class RecordSelect(a:Expr, b: name) extends Expr          // selecting value of label: `r.a`
  case class RecordExtend(a: Map[name, List[Expr]], b: Expr) extends Expr // extending a record: `{a = 1 | r}`
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
  case class TRowExtend(a:Map[name, List[Ty]], b:Row) extends Ty // row extension: `<a : _ | ...>`
  type Row = Ty // the kind of rows - empty row, row variable, or row extension




  sealed trait TVal
  case class Unbound(a: id, b: level) extends TVal
  case class Link(a: Ty) extends TVal
  case class Generic(a: id) extends TVal

  def real_ty(ty:Ty):Ty = {
    ty match {
      case TVar(Ref(Link(ty))) => real_ty(ty)
      case ty => ty
    }
  }

//   'a list LabelMap.t -> 'a list LabelMap.t -> 'a list LabelMap.t
  def merge_label_maps[A,B](label_map1:Map[A, List[B]], label_map2:Map[A,List[B]]):Map[A,List[B]] = {
    val merged = label_map1.toSeq ++ label_map2.toSeq
    val grouped = merged.groupBy(_._1)
    val cleaned = grouped.mapValues(_.map(_._2).flatten.toList)
    cleaned
  }

//val match_row_ty : row -> ty list LabelMap.t * ty
  
  // Returns a label map with all field types and the type of the "rest",
  // which is either a type var or an empty row.
  def match_row_ty(ty:Ty):(Map[name,List[Ty]], Ty) = {
    ty match {
    case TRowExtend(label_ty_map, rest_ty) =>
      match_row_ty(rest_ty) match {
        case (rest_label_ty_map, rest_ty) if rest_label_ty_map == Map() =>
          (label_ty_map, rest_ty)
        case (rest_label_ty_map, rest_ty) =>
          (merge_label_maps(label_ty_map, rest_label_ty_map), rest_ty)
      }
    case TVar(Ref(Link(ty))) => match_row_ty(ty)
    case TVar(_) => (Map(), ty)
    case TRowEmpty => (Map(), TRowEmpty)
    case ty => throw new Exception("not a row")
    }
  }

//val add_distinct_labels :
//  'a LabelMap.t -> (LabelMap.key * 'a) list -> 'a LabelMap.t

  // Adds new bindings to a label map. Assumes all bindings (both
  // new and existing) are distinct.
  def add_distinct_labels(label_el_map:Map[name,List[Ty]], label_el_list:List[(name,List[Ty])]):Map[name,List[Ty]] = {
    label_el_list.foldLeft(label_el_map) {
      case (label_el_map, (label, el)) =>
        assert(!label_el_map.contains(label))
        label_el_map + (label -> el)
    }
  }

//val label_map_from_list : (LabelMap.key * 'a) list -> 'a LabelMap.t

  def label_map_from_list(label_el_list: List[(name,List[Ty])]): Map[name,List[Ty]] = {
    add_distinct_labels(Map[name,List[Ty]](), label_el_list)
  }

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
      case RecordExtend(label_expr_map, rest_expr) =>
          val label_expr_str =
            label_expr_map.toList.map{
              case (label, expr_list) =>
                  expr_list.map{ expr => label + " = " + f(false, expr) }.mkString(", ")
            }.mkString(", ")
          val rest_expr_str = rest_expr match {
            case RecordEmpty => ""
            case expr => " | " + f(false, expr)
          }
          "{" + label_expr_str + rest_expr_str + "}"
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
        case TRowExtend(_,_) =>
            val (label_ty_map, rest_ty) = match_row_ty(ty)
            val label_ty_str =
                label_ty_map.toList.map {
                  case (label, ty_list) =>
                      ty_list.map{ ty => label + " : " + f(false, ty)}.mkString(", ")
                }.mkString(", ")
            val rest_ty_str = real_ty(rest_ty) match {
              case TRowEmpty => ""
              case TRowExtend(_, _) => assert(false)
              case other_ty => " | " + f(false, other_ty)
            }
            label_ty_str + rest_ty_str
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
