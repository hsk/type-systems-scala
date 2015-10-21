package dhm
import util.parsing.combinator._

object parse extends RegexParsers {
  import Expr._
  import Infer._

  def replace_ty_constants_with_vars (new_var_fn:()=>Ty, var_name_list:List[String], ty:Ty):Ty = {
    val env = var_name_list.foldLeft(Map[String,Ty]()) {
      case(env, var_name) => env + (var_name -> new_var_fn())
    }
    
    def f(ty:Ty):Ty = {
      ty match {
        case TConst(name) => env.getOrElse(name, ty)
        case TVar(_) => throw new Exception("assert")
        case TDynamic => TDynamic
        case TApp(ty, ty_arg_list) =>
          TApp(f(ty), ty_arg_list.map(f))
        case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(f), f(return_ty))
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

  val keywords = Set("fun", "let", "in", "forall", "some", "_")
  val ident = """[_A-Za-z][_A-Za-z0-9]*""".r ^? { case a if !keywords.contains(a) => a }
  val integer = """[0-9]+""".r

  val expr:Parser[Expr]
    = (app_expr ~ opt(":" ~> ty_ann) ^^ {case a~None=>a case a~Some(b)=> Ann(a, b) }).
    | (("let" ~> ident) ~ opt(":" ~> ty_ann) ~ ("=" ~> expr) ~ ("in" ~> expr) ^^ { case a~b~c~d => Let(a, b, c, d) } ).
    | (("fun" ~> rep(param)) ~ ("->" ~> expr)           ^^ { case a~b => Fun(a, b) } )

  lazy val param
    = (ident ^^ { (_, None) }).
    | ("(" ~> ident ~ (":" ~> ty_ann) <~ ")" ^^ { case a~b => (a, Some(b)) })

  lazy val app_expr
    = (simple_expr ~ rep("(" ~> rep1sep(expr, ",") <~ ")") ^^ { case a~b => b.foldLeft(a){(a,b)=>Call(a, b) } })

  lazy val simple_expr:Parser[Expr]
    = (ident               ^^ { Var(_) } ).
    | ("(" ~> expr <~ ")")

  val ty_forall
    = ty.
    | (("forall" ~> "[" ~> rep1(ident) <~ "]") ~ ty    ^^ { case a~b => replace_ty_constants_with_vars(new_gen_var, a, b) })

  lazy val ty:Parser[Ty]
    = (app_ty ~ ("->" ~> ty)                           ^^ { case a~b => TArrow(List(a), b) }).
    | (app_ty).
    | (("(" ~> repsep(ty, ",") <~ ")") ~ ("->" ~> ty)  ^^ { case a~b => TArrow(a, b) })

  lazy val ty_ann
    = ty.
    | ("some" ~> ("[" ~> rep1(ident) <~ "]") ~ ty ^^ { case a~b => replace_ty_constants_with_vars(new_gen_var, a, b) }).
    | ("_"                            ^^ { _ => new_gen_var() })

  lazy val app_ty
    = (simple_ty ~ rep("[" ~> rep1sep(ty, ",") <~ "]") ^^ { case a~b => b.foldLeft(a){(a,b)=>TApp(a, b)} })

  lazy val simple_ty:Parser[Ty]
    = (ident                                           ^^ { TConst(_) }).
    | ("(" ~> ty <~ ")").
    | ("?" ^^ {_=> TDynamic })
}