package refined

object Test_infer {

  import Expr._
  import Printing._

  sealed trait Result
  case class OK(a:String) extends Result
  case class Fail(a:Option[String]) extends Result

  val fail = Fail(None)
  def error(msg:String):Result = Fail(Some(msg))
  val test_infer = List(
    // Hindley-Milner
    "id" -> OK("forall[a] a -> a"),
    "1" -> OK("int"),
    "x" -> error("variable x not found"),
    "let x = x in x" -> error("variable x not found"),
    "let x = id in x" -> OK("forall[a] a -> a"),
    "let x = fun(y) -> y in x" -> OK("forall[a] a -> a"),
    "fun x -> x" -> OK("forall[a] a -> a"),
    "fun x -> x" -> OK("forall[int] int -> int"),
    "pair" -> OK("forall[a b] (a, b) -> pair[a, b]"),
    "pair" -> OK("forall[z x] (x, z) -> pair[x, z]"),
    "fun x -> let y = fun z -> z in y" -> OK("forall[a b] a -> b -> b"),
    "let f = fun x -> x in let id2 = fun y -> y in f == id" -> OK("bool"),
    "let f = fun x -> x in f == succ" -> OK("bool"),
    "let f = fun x -> x in pair(f(1), f(true))" -> OK("pair[int, bool]"),
    "fun f -> pair(f(1), f(true))" -> fail,
    "let f = fun(x, y) -> let a = x == y in x == y in f" -> OK("forall[a] (a, a) -> bool"),
    "id(id)" -> OK("forall[a] a -> a"),
    "choose(fun(x, y) -> x, fun(x, y) -> y)" -> OK("forall[a] (a, a) -> a"),
    "plain_choose_curry(fun(x, y) -> x)(fun(x, y) -> y)" -> OK("forall[a] (a, a) -> a"),
    "let x = id in let y = let z = x(id) in z in y" -> OK("forall[a] a -> a"),
    "cons(id, nil)" -> OK("forall[a] list[a -> a]"),
    "cons_curry(id)(nil)" -> OK("forall[a] list[a -> a]"),
    "let lst1 = cons(id, nil) in let lst2 = cons(succ, lst1) in lst2" -> OK("list[int -> int]"),
    "cons_curry(id)(cons_curry(succ)(cons_curry(id)(nil)))" -> OK("list[int -> int]"),
    "1 + true" -> error("cannot unify types int and bool"),
    "pair(1)" -> error("unexpected number of arguments"),
    "fun x -> let y = x in y" -> OK("forall[a] a -> a"),
    "fun x -> let y = let z = x(fun w -> w) in z in y" -> OK("forall[a b] ((a -> a) -> b) -> b"),
    "fun x -> fun y -> let z = x(y) in z(y)" -> OK("forall[a b] (a -> a -> b) -> a -> b"),
    "fun x -> let y = fun z -> x(z) in y" -> OK("forall[a b] (a -> b) -> a -> b"),
    "fun x -> let y = fun z -> x in y" -> OK("forall[a b] a -> b -> a"),
    "fun x -> fun y -> let z = x(y) in fun w -> y(w)" ->
      OK("forall[a b c] ((a -> b) -> c) -> (a -> b) -> a -> b"),
    "fun x -> let y = x in y(y)" -> error("recursive types"),
    "fun x -> let y = fun z -> z in y(y)" -> OK("forall[a b] a -> b -> b"),
    "fun x -> x(x)" -> error("recursive types"),
    "1(id)" -> error("expected a function"),
    "fun f -> let x = fun(g, y) -> let m = g(y) in f == g in x" ->
      OK("forall[a b] (a -> b) -> (a -> b, a) -> bool"),
    "let const = fun x -> fun y -> x in const" -> OK("forall[a b] a -> b -> a"),
    "let apply = fun(f, x) -> f(x) in apply" -> OK("forall[a b] (a -> b, a) -> b"),
    "let apply_curry = fun f -> fun x -> f(x) in apply_curry" -> OK("forall[a b] (a -> b) -> a -> b"),
    // type-checking contracts
    "let g = fun(f : int -> int if f(true) == 1) -> 1 in g" -> error("cannot unify types int and bool"),
    "choose(length, length)" -> OK("forall[a] array[a] -> int"),
    "1 : int if 1 > 0" -> OK("int"),
    "1 : int if 1 + 0" -> error("cannot unify types int and bool"),
    "fun(x : some[a] a if x, y : some[a] a) : (z : bool if y) -> x" -> OK(" (bool, bool) -> bool"),
    "fun(a) : (f : int -> int if f(a) == 1) -> fun b -> 1" -> OK("int -> int -> int"),
    "let const_1 = make_const(1) in const_1" -> OK("forall[a] a -> int"),

    // This one ideally shouldn't fail, but this system doesn't permit duplicate variables.
    "let x = 0 in fac : (x : int if x >= 0) -> int" -> fail
  )

  val test_infer_and_syntax = List(
    // Function string_of_t_expr prints a typed syntax tree where all function
    // arguments already have known types, so in all these test cases all parameters
    // must have fully annotated types.
    "fun(x : int) -> x + 1" -> OK("int -> int"),
    "fun() : ((x : int if x > 0) -> int) -> id" -> OK("() -> int -> int"),
    "fun() : (x : int if x > 0) -> 1" -> OK("() -> int"),
    "fun(a : int) : (f : int -> int if f(a) == 1) -> fun(b : int) -> 1" -> OK("int -> int -> int")
  )

  def string_of_result(r: Result): String = {
    r match {
    case Fail(None) => "Fail"
    case Fail(Some(msg)) => "Fail " + msg
    case OK(ty_str) => "OK " + ty_str
    }
  }

  def normalize(ty_str:String): String = {
    string_of_s_ty(Parse.ty_forall_eof(ty_str))
  }

  def cmp_result(result1:Result, result2:Result):Boolean = {
    (result1, result2) match {
      case (Fail(None), Fail(_)) => true
      case (Fail(_), Fail(None)) => true
      case (Fail(Some(msg1)), Fail(Some(msg2))) => msg1 == msg2
      case (OK(ty_str1), OK(ty_str2)) => normalize(ty_str1) == normalize(ty_str2)
      case (_, _) => false
    }
  }

  def assert_equal(str:String, er:s_expr, r:s_expr) {
    if(er != r)
      println("source "+str+" expected "+string_of_s_expr(er)+" but result is "+string_of_s_expr(r))
  }

  def assert_equal2(str:String, er:Result, r:Result) {
    if(!cmp_result(er, r))
      println("source "+str+" expected "+string_of_result(er)+" but result is "+string_of_result(r))
  }

  def make_single_test_case(check_typed_syntax:Boolean, code:String, expected_result:Result) {
    Infer.reset_id()
    val original_s_expr = Parse.expr_eof(code)
    val (result, maybe_t_expr) = {
      try {
        val t_expr = Infer.infer_expr(Core.plain_env, 0, original_s_expr)
        val t_ty = t_expr.ty
        val generalized_ty = Infer.generalize(-1, t_ty)
        (OK(string_of_t_ty(generalized_ty)), Some(t_expr))
      } catch {
        case e:Exception =>
        (Fail(Some(e.getMessage())), None)
      }
    }
    assert_equal2(code, expected_result, result)
    if (check_typed_syntax) {
      maybe_t_expr match {
        case Some(t_expr) =>
          val t_expr_str = string_of_t_expr(t_expr)
          Infer.reset_id()
          try {
            val new_s_expr = Parse.expr_eof(t_expr_str)
            assert_equal(t_expr_str, original_s_expr, new_s_expr)
          } catch {
            case e:Throwable =>
              println("string_of_t_expr parsing error: " + t_expr_str)
          }
        case None =>
      }
    }
  }

  def apply() {
    test_infer.foreach{case (a,b) => make_single_test_case(false,a,b)}
    test_infer_and_syntax.foreach{case (a,b) => make_single_test_case(true,a,b)}
  }

}
