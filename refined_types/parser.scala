package refined

import util.parsing.combinator._

object Parse extends RegexParsers {
  import Expr._
  import Infer._

  val keywords = Set("true","false","if","then","else","let","in","some","forall","fun","and","or","not")

  val ident = """[A-Za-z][_A-Za-z0-9]*""".r ^? { case a if (!keywords.contains(a)) => a }
  val int = """[0-9]+""".r ^^ { _.toInt }

  def unary(op: String, arg: s_expr): s_expr = {
    SCall(SVar(op), List(arg))
  }

  def binary(op: String, left: s_expr, right: s_expr): s_expr = {
    SCall(SVar(op), List(left, right))
  }

  def replace_ty_constants_with_vars(var_name_list: List[String], ty: s_ty): s_ty = {
    val env = var_name_list.foldLeft(Map[String,s_ty]()) {
      case(env, var_name) =>
        env + (var_name -> new_gen_var()) 
    }
    def f(ty:s_ty):s_ty = {
      ty match {
        case TConst(name) =>
          try {
            env(name)
          } catch {
            case _:Throwable => ty
          }
        case TApp(name, arg_ty_list) => TApp(name, arg_ty_list.map(f))
        case TArrow(param_r_ty_list, return_r_ty) =>
          val g = {a:s_refined_ty=>r_ty_map(f, a)}
          TArrow(param_r_ty_list.map(g), g(return_r_ty))
        case TVar(_) => ty
      }
    }
    f(ty)
  }

  def parse[A](p:Parser[A],s:String):A = {
    parseAll(p, s) match {
      case Success(t, _) => t
      case e => throw new Exception(e.toString)
    }
  }

  def expr_eof(code:String):s_expr = parse(expr, code)

  def ty_eof(code:String):s_ty = parse(ty, code)

  def ty_forall_eof(code:String):s_ty = parse(ty_forall, code)

  lazy val expr:Parser[s_expr]
    = ((app_expr <~ ":") ~ ty ~ opt("if" ~> expr)   ^^ { case a~b~c => SCast(a, b, c) }).
    | (("let" ~> ident) ~ ("=" ~> expr) ~ ("in" ~> expr) ^^ { case a~b~c => SLet(a, b, c) }).
    | (boolean_expr).
    | (fun_expr).
    | (("if" ~> expr) ~ ("then" ~> expr) ~ ("else" ~> expr) ^^ { case a~b~c => SIf(a, b, c) })

  lazy val boolean_expr:Parser[s_expr]
    = ("not" ~> relation_expr ^^ { unary("not", _) }).
    | (relation_expr ~ opt(("and"|"or") ~ relation_expr) ^^ {
        case a~b => b.foldLeft(a){case (a,b~c)=>binary(b, a, c)}
      })

  lazy val relation_expr
    = (arithmetic_expr ~ rep(relation_op ~ arithmetic_expr) ^^ {
        case a~b => b.foldLeft(a){case (a,b~c)=>binary(b, a, c)}
      })

  lazy val arithmetic_expr:Parser[s_expr]
    = (mul_expr ~ rep(("+"|"-") ~ mul_expr) ^^ {
        case a~b => b.foldLeft(a){case(a,op~b)=> binary(op, a, b)}
      })

  lazy val mul_expr:Parser[s_expr]
    = (unary_expr ~ rep(("*"|"/"|"%") ~ unary_expr) ^^ {
        case a~b => b.foldLeft(a){case(a,op~b)=> binary(op, a, b)}
      })

  lazy val unary_expr:Parser[s_expr]
    = ("-" ~> unary_expr ^^ { unary("unary-", _) }).
    | (app_expr)

  lazy val app_expr:Parser[s_expr]
    = (simple_expr ~ rep("(" ~> repsep(expr, ",") <~ ")") ^^ {
        case a~b => b.foldLeft(a){case(a,b)=>SCall(a, b)}
      })

  lazy val simple_expr:Parser[s_expr]
    = (ident                ^^ { SVar(_) }).
    | (int                  ^^ { SInt(_) }).
    | ("true"               ^^ { _ => SBool(true) }).
    | ("false"              ^^ { _ =>SBool(false) }).
    | ("(" ~> expr <~ ")")

  lazy val relation_op
    = "<=" | ">=" | "<" | ">" | "==" | "!="

  lazy val fun_expr
    = (("fun" ~> ident <~ "->") ~ expr ^^ {
        case a~b => SFun(List(s_param(a, None)), None, b)
      }).
    | (("fun" ~> "(" ~> param_list <~ ")") ~ opt(":" ~> return_ty) ~ ("->" ~> expr) ^^ {
        case a~b~c => SFun(a, b, c)
      })

  lazy val param_list:Parser[List[s_param]]
    = repsep(param, ",")

  lazy val param:Parser[s_param]
    = (ident ~ (":" ~> ty) ~ opt("if" ~> expr) ^^ { case a~b~c => s_param(a, Some(b, c)) }).
    | (ident                                   ^^ { s_param(_, None) })

  lazy val return_ty
    = (some_simple_ty                                       ^^ { Plain(_) }).
    | (("(" ~> ident) ~ (":" ~> ty <~ ")")                  ^^ { case a~b => Named(a, b) }).
    | (("(" ~> ident) ~ (":" ~> ty) ~ ("if" ~> expr) <~ ")" ^^ { case a~b~c => Refined(a, b, c) })

  lazy val ident_list:Parser[List[String]]
    = rep1(ident)

  lazy val ty_forall:Parser[s_ty]
    = ty.
    | (("forall" ~> "[" ~> ident_list <~ "]") ~ ty ^^ {
        case a~b => replace_ty_constants_with_vars(a, b)
      })

  lazy val ty:Parser[s_ty]
    = (function_ty).
    | (simple_ty).
    | (("some" ~> "[" ~> ident_list <~ "]") ~ ty ^^ {
        case a~b => replace_ty_constants_with_vars(a, b)
      })

  lazy val function_ty
    = ("(" ~> ")" ~> "->" ~> function_ret_ty ^^ { TArrow(List(), _) }).
    | (simple_ty ~ ("->" ~> function_ret_ty) ^^ { case a~b => TArrow(List(Plain(a)), b) }).
    | (("(" ~> refined_ty <~ ")" <~ "->") ~ function_ret_ty ^^ { case a~b => TArrow(List(a), b) }).
    | (("(" ~> param_ty <~ ",") ~ param_ty_list ~ (")" ~> "->" ~> function_ret_ty) ^^ { case a~b~c => TArrow(a :: b, c) })

  lazy val function_ret_ty
    = (ty ^^ { Plain(_) }).
    | ("(" ~> refined_ty <~ ")")

  lazy val param_ty_list
    = rep1sep(param_ty, ",")

  lazy val param_ty
    = (refined_ty).
    | (ty                     ^^ { Plain(_) })

  lazy val refined_ty
    = ((ident <~ ":") ~ ty ~ opt("if" ~> expr) ^^ {
      case a~b~None => Named(a, b)
      case a~b~Some(c) => Refined(a, b, c)
    })

  lazy val some_simple_ty
    = simple_ty.
    | (("some" ~> "[" ~> ident_list <~ "]") ~ simple_ty ^^ {
        case a~b => replace_ty_constants_with_vars(a, b)
      })

  lazy val simple_ty
    = (ident ~ ("[" ~> rep1sep(ty, ",") <~ "]")   ^^ { case a~b => TApp(a, b) }).
    | (ident                                      ^^ { TConst[s_expr](_) }).
    | ("(" ~> ty <~ ")")
}