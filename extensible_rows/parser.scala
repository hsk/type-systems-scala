package dhm
import util.parsing.combinator._

object parse extends RegexParsers {
  import Expr._
  import Infer._

  def expr_record_extend(label_expr_list:List[(String, Expr)], record:Expr):Expr = {
    label_expr_list.foldLeft(record) {
      case (record, (label, expr)) => RecordExtend(label, expr, record)
    }
  }

  def ty_row_extend(label_ty_list:List[(String, Ty)], row:Ty):Ty = {
    label_ty_list.foldLeft(row) {
      case (row, (label, ty)) => TRowExtend(label, ty, row)
    }
  }

  def replace_ty_constants_with_vars (var_name_list:List[String], ty:Ty):Ty = {
    val env = var_name_list.foldLeft(Env.empty) {
      case(env, var_name) => Env.extend(env, var_name, new_gen_var())
    }
    
    def f(ty:Ty):Ty = {
      ty match {
        case TConst(name) =>
          try {
            Env.lookup(env, name)
          } catch {
            case _:Throwable => ty
          }
        case TVar(_) => ty
        case TApp(ty, ty_arg_list) =>
          TApp(f(ty), ty_arg_list.map(f))
        case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(f), f(return_ty))
        case TRecord(row) => TRecord(f(row))
        case TRowEmpty => ty
        case TRowExtend(label, ty, row) => TRowExtend(label, f(ty), f(row))
      }
    }
    f(ty)
  }

  def parse[T](p:Parser[T],s:String):T = parseAll(p, s) match {
    case Success(t, _) => t
    case e => throw new Exception(e.toString)
  }
  def expr_eof(s:String):Expr = parse(expr, s)
  def ty_eof(s:String):Ty = parse(ty, s)
  def ty_forall_eof(s:String):Ty = parse(ty_forall, s)

  val keywords = Set("fun", "let", "in", "forall")
  val ident = """[_A-Za-z][_A-Za-z0-9]*""".r ^? { case a if !keywords.contains(a) => a }
  val integer = """[0-9]+""".r

  val expr:Parser[Expr]
    = (simple_expr ~ rep(app_rep) ^^ { case a~b => b.foldLeft(a){(a,b)=>b(a)} }).
    | (("let" ~> ident) ~ ("=" ~> expr) ~ ("in" ~> expr) ^^ { case a~b~c => Let(a, b, c) } ).
    | (("fun" ~> rep1(ident)) ~ ("->" ~> expr)           ^^ { case a~b => Fun(a, b) } )


  lazy val record_label_expr_list
    = rep1sep(ident ~ ("=" ~> expr) ^^ {case a~b => (a,b)}, ",") ^^ {_.reverse}

  def app_rep
    = ("(" ~> rep1sep(expr, ",") <~ ")" ^^ { b => a:Expr => Call(a, b) }).
    | ("." ~> ident                     ^^ { b => a:Expr => RecordSelect(a, b) })

  lazy val simple_expr:Parser[Expr]
    = (ident               ^^ { Var(_) } ).
    | ("(" ~> expr <~ ")").
    | ("{" ~> "}"                                        ^^ { _ => RecordEmpty }).
    | ("{" ~> (record_label_expr_list ~ ("|" ~> expr)) <~ "}" ^^ { case a~b => expr_record_extend(a, b) }).
    | ("{" ~> record_label_expr_list <~ "}"              ^^ { expr_record_extend(_, RecordEmpty) }).
    | ("{" ~> ((expr <~ "-") ~ ident) <~ "}"             ^^ { case a~b => RecordRestrict(a, b) })

  val ty_forall
    = ty.
    | (("forall" ~> "[" ~> rep1(ident) <~ "]") ~ ty    ^^ { case a~b => replace_ty_constants_with_vars(a, b) })

  lazy val ty:Parser[Ty]
    = (app_ty ~ ("->" ~> ty)                           ^^ { case a~b => TArrow(List(a), b) }).
    | (app_ty).
    | (("(" ~> repsep(ty, ",") <~ ")") ~ ("->" ~> ty)  ^^ { case a~b => TArrow(a, b) })

  lazy val ty_row
    = rep1sep(ident ~ (":" ~> ty) ^^ { case a~b => (a, b) }, ",")

  lazy val app_ty
    = (simple_ty ~ rep("[" ~> rep1sep(ty, ",") <~ "]") ^^ { case a~b => b.foldLeft(a){(a,b)=>TApp(a, b)} })

  lazy val simple_ty:Parser[Ty]
    = (ident                                           ^^ { TConst(_) }).
    | ("(" ~> ty <~ ")").
    | ("{" ~ "}"                               ^^ { _ => TRecord(TRowEmpty) }).
    | ("{" ~> ident <~ "}"                     ^^ { a => TRecord(TConst(a)) }).
    | ("{" ~> (ty_row ~ ("|" ~> ty)) <~ "}"    ^^ { case a~b => TRecord(ty_row_extend(a, b)) }).
    | ("{" ~> ty_row <~ "}"                    ^^ { a => TRecord(ty_row_extend(a, TRowEmpty)) })

}