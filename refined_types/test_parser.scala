package refined

object Test_parser {

  import Expr._
  import Printing._

  sealed trait Result
  case class OK(a:s_expr) extends Result
  case object Fail extends Result

  def bound[A](i:Int):Ty[A] = TVar(Generic[A](i))

  val test_cases = List(
    "" -> Fail,
    "a" -> OK(SVar("a")),
    "f(x, 1, true)" -> OK(SCall(SVar("f"), List(SVar("x"), SInt(1), SBool(true)))),
    "f(x)(false)" -> OK(SCall(SCall(SVar("f"), List(SVar("x"))), List(SBool(false)))),
    "let f = fun(x, y : int) -> g(x > -2, y + 1) in f(a, b)" ->
      OK(SLet("f",
        SFun(
            List(
                s_param("x", None),
                s_param("y", Some((TConst("int"), None)))
            ),
            None,
            SCall(
                SVar("g"),
                List(
                    SCall(SVar(">"), List(SVar("x"), SCall(SVar("unary-"), List(SInt(2)) )) ),
                    SCall(SVar("+"), List(SVar("y"), SInt(1)))
                )
            )
        ),
        SCall(SVar("f"), List(SVar("a"), SVar("b")))
      )),
    "let x = a : array[byte] in let y = b : int if b > 0 in f(x, y)" ->
      OK(SLet("x", SCast(SVar("a"), TApp("array", List(TConst("byte"))), None),
        SLet("y", SCast(SVar("b"), TConst("int"), Some(SCall(SVar(">"), List(SVar("b"), SInt(0)) ))),
        SCall(SVar("f"), List(SVar("x"), SVar("y")) )))),
    "a : int" -> OK(SCast(SVar("a"), TConst("int"), None)),
    "a : int if a > 0" ->
      OK(SCast(SVar("a"), TConst("int"), Some(SCall(SVar(">"), List(SVar("a"), SInt(0) ) )))),
    "1 : int if a > 0" ->
      OK(SCast(SInt(1), TConst("int"), Some(SCall(SVar(">"), List(SVar("a"), SInt(0)) )))),
    "f x" -> Fail,
    "f x" -> Fail,
    "let a = one" -> Fail,
    "a, b" -> Fail,
    "a = b" -> Fail,
    "()" -> Fail,
    "fun x, y -> y" -> Fail,
    "fun x y -> y" -> Fail,
    "a and b or c" -> Fail,
    "(a and b) or c" -> OK(SCall(SVar("or"), List(SCall(SVar("and"), List(SVar("a"), SVar("b")) ), SVar("c")) )),
    "not a or b" -> Fail,
    "(not a > 0) or b" ->
      OK(SCall(SVar("or"), List(SCall(SVar("not"), List(SCall(SVar(">"), List(SVar("a"), SInt(0)) ))), SVar("b")) )),
    "fun() : int -> y" -> OK(SFun(List(), Some(Plain(TConst("int"))), SVar("y"))),
    "1 : int" -> OK(SCast(SInt(1), TConst("int"), None)),
    "1 : some[a] a" -> OK(SCast(SInt(1), bound(0), None)),
    "1 : some[a] array[a]" -> OK(SCast(SInt(1), TApp("array", List(bound(0))), None)),
    "1 : some[a b] pair[a, pair[b, array[a]]]" ->
      OK(SCast(SInt(1), TApp("pair", List(bound(0), TApp("pair",
        List(bound(1), TApp("array", List(bound(0)) )) ))), None)),
    "id : some[a] a -> a" -> OK(SCast(SVar("id"), (TArrow(List(Plain(bound(0))), Plain(bound(0)))), None)),
    "fun(x : some[a] a, y : int -> int, z : (int -> int) if z(0) != 1) : some[a] array[a] -> 1" ->
      OK(SFun(List(
        s_param("x", Some(bound(0), None)),
        s_param("y", Some(TArrow(List(Plain(TConst("int"))),Plain(TConst("int"))), None)),
        s_param("z", Some(TArrow(List(Plain(TConst("int"))),Plain(TConst("int"))),
            Some(SCall(SVar("!="), List(SCall(SVar("z"), List(SInt(0)) ), SInt(1)) ))))
        ),
        Some(Plain(TApp("array", List(bound(1)) ))), SInt(1))),
    "fun(f : (x : int if x != 0) -> (y : int if x != y)) -> f(0)" ->
      OK(SFun(List(
          s_param("f", Some(TArrow(List(Refined("x", TConst("int"), SCall(SVar("!="),
          List(SVar("x"), SInt(0)) ))), Refined("y", TConst("int"), SCall(SVar("!="),
          List(SVar("x"), SVar("y")) ))), None))
        ),
        None, SCall(SVar("f"), List(SInt(0)) ) )),
    "b : some[a] array[a] if length(b) > 0" ->
      OK(SCast(SVar("b"), TApp("array", List(bound(0)) ), Some(SCall(SVar(">"), List(SCall(SVar
        ("length"), List(SVar("b")) ), SInt(0)) )))),
    "plus : (x : int, y : int) -> (z : int if z == x + y)" ->
      OK(SCast(SVar("plus"), TArrow(List(Named("x", TConst("int")), Named("y", TConst("int")) ),
        Refined("z", TConst("int"), SCall(SVar("=="), List(SVar("z"), SCall(SVar("+"),
        List(SVar("x"), SVar("y")) )) ))), None)),
    "f : (x : int if x > 0) -> ((y : int) -> (z : int if z == x + y))" ->
      OK(SCast(SVar("f"), TArrow(List(Refined("x", TConst("int"), SCall(SVar(">"), List(SVar("x"),
        SInt(0))))), Plain(TArrow(List(Named("y", TConst("int"))), Refined("z", TConst("int"),
        SCall(SVar("=="), List(SVar("z"), SCall(SVar("+"), List(SVar("x"), SVar("y")) )) ))))), None)),
    "fun() : ((x : int if x > 0) -> int) -> id" ->
      OK(SFun(List(), Some(Plain(TArrow(List(Refined("x", TConst("int"), SCall(SVar(">"),
        List(SVar("x"), SInt(0))))), Plain(TConst("int"))))), SVar("id"))),
    "fun(f : (int -> (x : int if x > 0)) if f(0) > 1) -> 1" ->
      OK(SFun(
        List(
          s_param("f", Some(
            TArrow(
              List(Plain(TConst("int"))),
              Refined("x", TConst("int"), SCall(SVar(">"), List(SVar("x"), SInt(0))))),
            Some(SCall(SVar(">"), List(SCall(SVar("f"), List(SInt(0))), SInt(1))))
          ))
        ), None, SInt(1)
      ))
  )

  def string_of_result(r:Result): String = {
    r match {
      case Fail => "Fail"
      case OK(s_expr) => "OK (" + string_of_s_expr(s_expr) + ")"
    }
  }

  def assert_equal(str:String, er:Result, r:Result, err:Option[Exception]) {
    if(er != r)
      println(
        "source "+str+" expected "+string_of_result(er)+
        " but result is "+string_of_result(r)+"\n"+err.getOrElse(new Exception("")).getMessage())
  }

  def make_single_test_case(code:String, expected_result:Result) {
    Infer.reset_id()

    val (result, err) =
      try {
        (OK(Parse.expr_eof(code)), None)
      } catch {
        case e:Exception =>
          (Fail, Some(e))
      }
    assert_equal(code, expected_result, result, err)
    result match {
      case OK(s_expr) =>
        val s_expr_str = string_of_s_expr(s_expr)
        Infer.reset_id()
        try {
            val new_result = OK(Parse.expr_eof(s_expr_str))
            assert_equal(code,expected_result, new_result, None)
        } catch {
            case _:Exception => throw new Exception("string_of_s_expr parsing error: " + s_expr_str)
        }
      case Fail =>
    }
  }

  def apply() {
    test_cases.toList.foreach{case(a,b)=>make_single_test_case(a,b)}
  }

}
