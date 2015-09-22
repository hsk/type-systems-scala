package dhm

object Test_parser {

  import Expr._


  sealed trait Result
  case class OK(a:Expr) extends Result
  case object Fail extends Result


  val test_cases = Map(
    "" -> Fail,
    "a" -> OK(Var("a")),
    "f(x, y)" -> OK(Call(Var("f"), List(Var("x"), Var("y")))),
    "f(x)(y)" -> OK(Call(Call(Var("f"), List(Var("x"))), List(Var("y")))),
    "let f = fun x y -> g(x, y) in f(a, b)" ->
      OK(Let("f", Fun(List("x", "y"), Call(Var("g"), List(Var("x"), Var("y")))),
        Call(Var("f"), List(Var("a"), Var("b"))))),
    "let x = a in " +
     "let y = b in " +
     "f(x, y)" -> OK(Let("x", Var("a"), Let("y", Var("b"), Call(Var("f"), List(Var("x"), Var("y")))))),
    "f x" -> Fail,
    "let a = one" -> Fail,
    "a, b" -> Fail,
    "a = b" -> Fail,
    "()" -> Fail,
    "fun x, y -> y" -> Fail
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
    val result =
      try {
        OK(parse.expr_eof(code))
      } catch {
        case e:Throwable => Fail
      }
    assert_equal(code, expected_result, result)
  }

  def apply() {
    test_cases.foreach {
      case (a, b) => make_single_test_case(a, b)
    }
  }
}
