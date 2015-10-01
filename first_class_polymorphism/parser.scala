package fcp

import util.parsing.combinator._

object Parse extends RegexParsers {
  import Expr._
  import Infer._

  def replace_ty_constants_with_vars (var_name_list:List[String], ty: ty):(List[id], ty) = {
    var name_map = Map[String,Option[ty]]()
    val var_id_list_rev_ref = Ref(List[id]())

    var_name_list.foreach {
      (var_name) => name_map = name_map + (var_name -> None)
    }  
    
    def f(ty:ty):ty = {
      ty match {
      case TConst(name) =>
        try {
          name_map(name) match {
            case Some(v) => v
            case None =>
              val (var_id, v) = new_bound_var()
              var_id_list_rev_ref.a = var_id :: var_id_list_rev_ref.a
              name_map = name_map + (name -> Some(v))
              v
          }
        } catch {
          case _:Throwable => ty
        }
      case TVar(_) => ty
      case TApp(ty, ty_arg_list) =>
        val new_ty = f(ty)
        val new_ty_arg_list = ty_arg_list.map(f) 
        TApp(new_ty, new_ty_arg_list)
      case TArrow(param_ty_list, return_ty) =>
        val new_param_ty_list = param_ty_list.map(f)
        val new_return_ty = f(return_ty)
        TArrow(new_param_ty_list, new_return_ty)
      case TForall(var_id_list, ty) => TForall(var_id_list, f(ty))
      }
    }
    val ty2 = f(ty)
    (var_id_list_rev_ref.a.reverse, ty2)
  }

  def parse[T](p:Parser[T],s:String):T = parseAll(p, s) match {
    case Success(t, _) => t
    case e => throw new Exception(e.toString)
  }
  
  def expr_eof(code:String): expr = parse(expr, code)
  def ty_eof(code:String): ty = parse(ty, code)

  val keywords = Set("fun", "let", "in", "forall", "some")
  val ident = """[_A-Za-z][_A-Za-z0-9]*""".r ^? { case a if !keywords.contains(a) => a }
  val integer = """[0-9]+""".r

  lazy val expr:Parser[expr]
    = ("let" ~> (ident <~ "=") ~ expr ~ ("in" ~> expr) ^^ { case a~b~c => Let(a, b, c) }).
    | ("fun" ~> rep(param) ~ ("->" ~> expr)     ^^ { case a~b => Fun(a, b) }).
    | (app_expr ~ opt(":" ~> ty_ann)  ^^ { case a~None => a case a~Some(b) => Ann(a, b) })

  lazy val app_expr
    = (simple_expr ~ rep("(" ~> repsep(expr, ",") <~ ")") ^^ { case a~b => b.foldLeft(a){(a,b)=>Call(a, b) } })

  lazy val simple_expr:Parser[expr]
    = (ident                                         ^^ { Var(_) }).
    | ("(" ~> expr <~ ")")

  lazy val param
    = (ident                              ^^ { (_, None) }).
    | ("(" ~> ident ~ (":" ~> ty_ann) <~ ")" ^^ { case a~b => (a, Some(b)) })

  lazy val ident_list
    = rep1(ident)

  lazy val ty_ann
    = (ty                                    ^^ { (List(), _) }).
    | ("some" ~> ("[" ~> ident_list <~ "]") ~ ty  ^^ { case a~b => replace_ty_constants_with_vars(a, b) })

  lazy val ty:Parser[ty]
    = ty_plain.
    | ("forall" ~> ("[" ~> ident_list <~ "]") ~ ty_plain ^^ {
        case a~b =>
          val (var_id_list, ty) = replace_ty_constants_with_vars(a, b)
          var_id_list match {
            case List() => ty
            case _ => TForall(var_id_list, ty)
          }
        })

  lazy val ty_plain
    = (app_ty ~ opt("->" ~> ty)                  ^^ { case a~None => a case a~Some(b) => TArrow(List(a), b) }).
    | (("(" ~> repsep(ty, ",") <~ ")") ~ ("->" ~> ty) ^^ { case a~b => TArrow(a, b) })

  lazy val app_ty
    = (simple_ty ~ rep("[" ~> rep1sep(ty, ",") <~ "]") ^^ { case a~b => b.foldLeft(a){(a,b)=>TApp(a, b)} })

  lazy val simple_ty:Parser[ty]
    = (ident                                         ^^ { TConst(_) }).
    | ("(" ~> ty <~ ")")

  lazy val ty_comma_list
    = rep1sep(ty, ",")
  
}
