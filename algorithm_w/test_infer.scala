package dhm

object Test_infer {

  import Expr._

  sealed trait Result
  case class OK(a:String) extends Result
  case class Fail(a:Option[String]) extends Result

  val fail = Fail(None)
  def error(msg:String):Result = Fail(Some(msg))

  val test_cases = Map(
    // Hindley-Milner
    "id" -> OK("forall[a] a -> a"),
    "one" -> OK("int"),
    "x" -> error("variable x not found"),
    "let x = x in x" -> error("variable x not found"),
    "let x = id in x" -> OK("forall[a] a -> a"),
    "let x = fun y -> y in x" -> OK("forall[a] a -> a"),
    "fun x -> x" -> OK("forall[a] a -> a"),
    "fun x -> x" -> OK("forall[int] int -> int"),
    "pair" -> OK("forall[a b] (a, b) -> pair[a, b]"),
    "pair" -> OK("forall[z x] (x, z) -> pair[x, z]"),
    "fun x -> let y = fun z -> z in y" -> OK("forall[a b] a -> b -> b"),
    "let f = fun x -> x in let id = fun y -> y in eq(f, id)" -> OK("bool"),
    "let f = fun x -> x in let id = fun y -> y in eq_curry(f)(id)" -> OK("bool"),
    "let f = fun x -> x in eq(f, succ)" -> OK("bool"),
    "let f = fun x -> x in eq_curry(f)(succ)" -> OK("bool"),
    "let f = fun x -> x in pair(f(one), f(true))" -> OK("pair[int, bool]"),
    "fun f -> pair(f(one), f(true))" -> fail,
    "let f = fun x y -> let a = eq(x, y) in eq(x, y) in f" -> OK("forall[a] (a, a) -> bool"),
    "let f = fun x y -> let a = eq_curry(x)(y) in eq_curry(x)(y) in f" ->
      OK("forall[a] (a, a) -> bool"),
    "id(id)" -> OK("forall[a] a -> a"),
    "choose(fun x y -> x, fun x y -> y)" -> OK("forall[a] (a, a) -> a"),
    "choose_curry(fun x y -> x)(fun x y -> y)" -> OK("forall[a] (a, a) -> a"),
    "let x = id in let y = let z = x(id) in z in y" -> OK("forall[a] a -> a"),
    "cons(id, nil)" -> OK("forall[a] list[a -> a]"),
    "cons_curry(id)(nil)" -> OK("forall[a] list[a -> a]"),
    "let lst1 = cons(id, nil) in let lst2 = cons(succ, lst1) in lst2" -> OK("list[int -> int]"),
    "cons_curry(id)(cons_curry(succ)(cons_curry(id)(nil)))" -> OK("list[int -> int]"),
    "plus(one, true)" -> error("cannot unify types int and bool"),
    "plus(one)" -> error("unexpected number of arguments"),
    "fun x -> let y = x in y" -> OK("forall[a] a -> a"),
    "fun x -> let y = let z = x(fun x -> x) in z in y" -> OK("forall[a b] ((a -> a) -> b) -> b"),
    "fun x -> fun y -> let x = x(y) in x(y)" -> OK("forall[a b] (a -> a -> b) -> a -> b"),
    "fun x -> let y = fun z -> x(z) in y" -> OK("forall[a b] (a -> b) -> a -> b"),
    "fun x -> let y = fun z -> x in y" -> OK("forall[a b] a -> b -> a"),
    "fun x -> fun y -> let x = x(y) in fun x -> y(x)" ->
      OK("forall[a b c] ((a -> b) -> c) -> (a -> b) -> a -> b"),
    "fun x -> let y = x in y(y)" -> error("recursive types"),
    "fun x -> let y = fun z -> z in y(y)" -> OK("forall[a b] a -> b -> b"),
    "fun x -> x(x)" -> error("recursive types"),
    "one(id)" -> error("expected a function"),
    "fun f -> let x = fun g y -> let _ = g(y) in eq(f, g) in x" ->
      OK("forall[a b] (a -> b) -> (a -> b, a) -> bool"),
    "let const = fun x -> fun y -> x in const" -> OK("forall[a b] a -> b -> a"),
    "let apply = fun f x -> f(x) in apply" -> OK("forall[a b] (a -> b, a) -> b"),
    "let apply_curry = fun f -> fun x -> f(x) in apply_curry" -> OK("forall[a b] (a -> b) -> a -> b")
  )

  def string_of_result(r:Result):String = {
    r match {
      case Fail(None) => "Fail"
      case Fail(Some(msg)) => "Fail " + msg
      case OK(ty_str) => "OK " + ty_str
    }
  }

  def normalize(ty_str: String):String = {
    string_of_ty(parse.ty_forall_eof(ty_str))
  }

  def cmp_result(result1:Result, result2:Result):Boolean = {
    (result1, result2) match {
    case (Fail(None), Fail(_)) => true
    case (Fail(_), Fail(None)) => true
    case (Fail(Some(msg1)), Fail(Some(msg2))) => msg1 == msg2
    case (OK(ty_str1), OK(ty_str2)) => normalize(ty_str1) == normalize(ty_str2)
    case _ => false
    }
  }

  def assert_equal(str:String, er:Result, r:Result) {
    if(!cmp_result(er,r))
      println("source "+str+" expected "+string_of_result(er)+" but result is "+string_of_result(r))

  }

  def make_single_test_case(code:String, expected_result:Result) {
    val result = {
      try {
        Infer.reset_id()
        val ty = Infer.infer(Core.core, 0, parse.expr_eof(code))
        val generalized_ty = Infer.generalize(-1, ty)
        OK(string_of_ty(generalized_ty))
      } catch {
        case e:Exception => Fail(Some(e.getMessage()))
      }
    }
    assert_equal(code, expected_result, result)
  }

  def apply() {
    test_cases.foreach{
      case(a, b) => make_single_test_case(a, b)
    }
  }
}

