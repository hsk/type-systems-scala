package fcp

object Test_parser {

  import Expr._

  sealed trait Result
  case class OK(a:expr) extends Result
  case object Fail extends Result

  def bound(i:Int):ty = TVar(Ref(Bound(i)))

  val test_cases = List(
    "" -> Fail,
    "a" -> OK(Var("a")),
    "f(x, y)" -> OK(Call(Var("f"), List(Var("x"), Var("y")))),
    "f(x)(y)" -> OK(Call(Call(Var("f"), List(Var("x"))), List(Var("y")))),
    "let f = fun x y -> g(x, y) in f(a, b)" ->
      OK(Let("f", Fun(List("x" -> None, "y" -> None), Call(Var("g"), List(Var("x"), Var("y")))),
        Call(Var("f"), List(Var("a"), Var("b"))))),
    "let x = a in " +
     "let y = b in " +
     "f(x, y)" -> OK(Let("x", Var("a"), Let("y", Var("b"), Call(Var("f"), List(Var("x"), Var("y")))))),
    "f x" -> Fail,
    "let a = one" -> Fail,
    "a, b" -> Fail,
    "a = b" -> Fail,
    "()" -> Fail,
    "fun x, y -> y" -> Fail,
    "fun -> y" -> OK(Fun(List(), Var("y"))),
    "id : forall[a] a -> a" -> OK(Ann(Var("id"), (List(), TForall(List(0), TArrow(List(bound(0)), bound(0)))))),
    "magic : forall[a b] a -> b" ->
      OK (Ann(Var("magic"), (List(), TForall(List(0, 1), TArrow(List(bound(0)), bound(1)))))),
    "magic : forall[x int] x -> int" ->
      OK (Ann(Var("magic"), (List(), TForall(List(0, 1), TArrow(List(bound(0)), bound(1)))))),
    "magic : forall[w x y z] y -> x" ->
      OK (Ann(Var("magic"), (List(), TForall(List(0, 1), TArrow(List(bound(0)), bound(1)))))),
    "a : forall[a] forall[b] b" -> Fail,
    "a : (forall[int] int -> int) -> int" ->
      OK (Ann(Var("a"), (List(), TArrow(List(TForall(List(0), TArrow(List(bound(0)), bound(0)))), TConst("int"))))),
    "f : int -> int -> int" ->
      OK (Ann(Var("f"), (List(), TArrow(List(TConst("int")), TArrow(List(TConst("int")), TConst("int")))))),
    "f : some[a b] forall[c d] (int, int) -> int" ->
      OK (Ann(Var("f"), (List(), TArrow(List(TConst("int"), TConst("int")), TConst("int"))))),
    "fun (x : some[a] a -> a) y (z : (list[forall[t] t -> int])) -> m : int" ->
      OK (Fun(List(
        "x" -> Some((List(0), TArrow(List(bound(0)), bound(0)))),
        "y" -> None,
        "z" -> Some((List(), TApp(TConst("list"), List(TForall(List(1), TArrow(List(bound(1)), TConst("int")))))))
        ),
        Ann(Var("m"), (List(), TConst("int")))))
  )

  def string_of_result(r:Result):String = 
    r match {
      case Fail => "Fail"
      case OK(expr) => "OK (" + string_of_expr(expr) + ")"
    }

  def assert_equal(str:String, er:Result, r:Result) {
    if(er != r)
      println("source "+str+" expected "+string_of_result(er)+" but result is "+string_of_result(r))
  }

  def make_single_test_case(code:String, expected_result:Result) {
    Infer.reset_id()
    val result =
      try {
        OK(Parse.expr_eof(code))
      } catch {
        case e:Throwable => Fail
      }
    assert_equal(code, expected_result, result)
    
    result match {
      case OK(expr) =>
          val expr_str = string_of_expr(expr)
          Infer.reset_id()
          val new_result = OK(Parse.expr_eof(expr_str))
          assert_equal(code, expected_result, new_result)
      case Fail =>
    }
  }

  def apply() {
    test_cases.foreach {
      case (a, b) => make_single_test_case(a, b)
    }
  }


}
