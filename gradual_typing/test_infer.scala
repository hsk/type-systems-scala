package dhm

object Test_infer {

  import Expr._
  import Infer._

  sealed trait Result
  case class OK(a:String) extends Result
  case class Fail(a:Option[String]) extends Result

  val fail = Fail(None)
  def error(msg:String):Result = Fail(Some(msg))

  val test_hm_static = List(
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
    "fun f -> let x = fun g y -> let z = g(y) in eq(f, g) in x" ->
      OK("forall[a b] (a -> b) -> (a -> b, a) -> bool"),
    "let const = fun x -> fun y -> x in const" -> OK("forall[a b] a -> b -> a"),
    "let apply = fun f x -> f(x) in apply" -> OK("forall[a b] (a -> b, a) -> b"),
    "let apply_curry = fun f -> fun x -> f(x) in apply_curry" -> OK("forall[a b] (a -> b) -> a -> b"),

    "let f = fun x y -> pair(x, y) in f" -> OK("forall[a b] (a, b) -> pair[a, b]"),
    "let f = fun x y -> (pair(x, y) : some[a] pair[a, a]) in f" ->
      OK("forall[a] (a, a) -> pair[a, a]"),
    "let f : some[a] (a, a) -> pair[a, a] = fun x y -> pair(x, y) in f" ->
      OK("forall[a] (a, a) -> pair[a, a]"),
    "let f : some[a] a -> a = succ in f" -> OK("int -> int")
  )

  val test_dynamic = List[(String,Result)](
    "fun x -> x" -> OK("? -> ?"),
    "fun (x : int) -> x" -> OK("int -> int"),
    "fun (x : int) -> plus(x, one)" -> OK("int -> int"),
    "fun x -> plus(x, one)" -> OK("? -> int"),
    "fun (x : int) (g : ? -> ?) -> g(x)" -> OK("(int, ? -> ?) -> ?"),
    "fun (x : int) g -> g(x)" -> OK("(int, ?) -> ?"),
    "fun (x : int) (g : ? -> ?) -> plus(g, g(x))" -> fail,
    "fun (x : int) g -> plus(g, g(x))" -> OK("(int, ?) -> int"),
    "fun (g : _) -> g(one)" -> OK("forall[a] (int -> a) -> a"),
    "let x = one in x" -> OK("int"),
    "fun x -> let y = x in y" -> OK("? -> ?"),
    "fun (y : _) -> test(y, y)" -> OK("(int -> int) -> int"),
    "fun (y : _) -> test_curry(y)(y)" -> OK("(int -> int) -> int"),
    "fun (y : _) -> let z = test_curry(y) in y" -> OK("(? -> int) -> (? -> int)"),
    "fun (y : ? -> int) -> let z = test_curry(y) in y" -> OK("(? -> int) -> (? -> int)"),
    "fun (y : _) -> let z = test_curry(y) in let x = z(y) in y" -> OK("(int -> int) -> (int -> int)"),
    "fun (y : ? -> int) -> let z = test_curry(y) in let x = z(y) in y" ->
      OK("(? -> int) -> (? -> int)"),
    "fun -> " +
     " let g = fun (x : _) -> dynamic_to_int(x) in " +
     " let y = g(one) in " +
     " g" -> OK("() -> ? -> int"),
    "fun -> " +
     "(fun (g : _) -> " +
     "  (fun (y : _) -> g)(g(one))) " +
     " (fun (x : _) -> dynamic_to_int(x))" -> OK("() -> int -> int"),
    "fun (x : some[a] a -> a) -> x(one)" -> OK("(int -> int) -> int"),
    "fun (x : some[a] a -> a) -> dynamic_to_int(x)" -> OK("(? -> ?) -> int"),
    "fun (x : some[a] list[a]) -> dynamic_to_int(x)" -> OK("list[?] -> int"),
    "fun (x : some[a] list[a]) -> let z = dynamic_to_int(x) in sum(x)" -> OK("list[int] -> int"),
    "fun (x : list[?]) -> let z = dynamic_to_int(x) in sum(x)" -> OK("list[?] -> int"),
    "fun (x : _) -> dynamic_to_int(head(x))" -> OK("list[?] -> int"),
    "fun x -> sum(x)" -> OK("? -> int"),
    "fun (x : some[a] pair[a, a]) -> dynamic_to_int(x)" -> OK("pair[?, ?] -> int"),
    "fun (x : some[a b] pair[a, b]) -> dynamic_to_int(x)" -> OK("pair[?, ?] -> int"),
    "fun f -> pair(f(one), f(true))" -> OK("? -> pair[?, ?]"),
    "fun (f : some[a] a -> a) -> let x = dynamic_to_int(f) in pair(f(one), f(true))" ->
      error("cannot unify types int and bool"),
    "fun -> let f = fun (x : _) -> dynamic_to_int(x) in pair(f(one), f(true))" ->
      OK("() -> pair[int, int]"),
    "let f = (fun x -> x)(one) in pair(f(one), f(true))" -> OK("pair[?, ?]"),
    "fun (x : _) -> eq(x, (fun (y : ?) -> y))" -> OK(" (? -> ?) -> bool"),
    "fun (x : _) (y : _) -> pair(eq(x, (fun (y : ?) -> y)), eq(x, y))" ->
      OK("(? -> ?, ? -> ?) -> pair[bool, bool]"),
    "fun (x : _) -> pair(eq(x, (fun (y : ?) -> y)), x(one))" -> OK("(int -> ?) -> pair[bool, ?]"),
    "let x = (one : ?) in pair(x(one), pair(x(true), x))" -> OK("pair[?, pair[?, ?]]"),
    "let x : ? = one in pair(x(one), pair(x(true), x))" -> OK("pair[?, pair[?, ?]]"),
    "fun (x : int) -> cons(x, cons(dynamic, nil))" -> OK("int -> list[int]"),
    "fun (x : int) -> cons((x : ?), cons(dynamic, nil))" -> OK("int -> list[?]"),
    "let f : ? -> ? = one in f" -> fail,
    "let f : ? -> ? = dynamic in f" -> OK("? -> ?"),
    "let f : ? -> ? = (fun (x : _) -> x) in pair(f(one), f(true))" -> OK("pair[?, ?]"),
    "let f : ? -> ? = succ in f" -> OK("? -> ?"),
    "eq(pair(one, true), duplicate(dynamic))" -> OK("bool"),
    "let m = duplicate(dynamic) in eq(pair(one, true), m)" -> OK("bool"),
    "fun (x : some[a] pair[a, a]) -> " +
     " let b = eq(x, dynamic) in " +
     " eq(pair(one, true), id(x))" -> fail,
    "fun (x : some[a] pair[a, a]) -> " +
     " let b = eq(x, dynamic) in " +
     " let m = id(x) in " +
     " eq(pair(one, true), m)" -> fail
  )
  
  val test_dynamic_ann = List[(String,Result)](
    "fun (x : ?) -> x" -> OK("? -> ?"),
    "fun (x : ?) -> plus(x, one)" -> OK("? -> int"),
    "fun x (g : ? -> ?) -> g(x)" -> OK("(?, ? -> ?) -> ?"),
    "fun x (g : ?) -> g(x)" -> OK("(?, ?) -> ?"),
    "fun x (g : ? -> ?) -> plus(g, g(x))" -> fail,
    "fun x (g : ?) -> plus(g, g(x))" -> OK("(?, ?) -> int"),
    "fun (x : ?) -> let y = x in y" -> OK("? -> ?"),
    "fun y -> test(y, y)" -> OK("(int -> int) -> int"),
    "fun y -> test_curry(y)(y)" -> OK("(int -> int) -> int"),
    "fun y -> let z = test_curry(y) in y" -> OK("(? -> int) -> (? -> int)"),
    "fun (y : ? -> int) -> let z = test_curry(y) in y" -> OK("(? -> int) -> (? -> int)"),
    "fun y -> let z = test_curry(y) in let x = z(y) in y" -> OK("(int -> int) -> (int -> int)"),
    "fun (y : ? -> int) -> let z = test_curry(y) in let x = z(y) in y" ->
      OK("(? -> int) -> (? -> int)"),
    "fun -> " +
     " let g = fun x -> dynamic_to_int(x) in " +
     " let y = g(one) in " +
     " g" -> OK("() -> ? -> int"),
    "fun -> " +
     "(fun g -> " +
     "  (fun y -> g)(g(one))) " +
     " (fun x -> dynamic_to_int(x))" -> OK("() -> int -> int"),
    "fun x -> let z = dynamic_to_int(x) in sum(x)" -> OK("list[int] -> int"),
    "fun x -> let z = dynamic_to_int(x) in x" -> OK("? -> ?"),
    "fun (x : list[?]) -> let z = dynamic_to_int(x) in sum(x)" -> OK("list[?] -> int"),
    "fun (x : ?) -> sum(x)" -> OK("? -> int"),
    "fun (f : ?) -> pair(f(one), f(true))" -> OK("? -> pair[?, ?]"),
    "fun (f : some[a] a -> a) -> let x = dynamic_to_int(f) in pair(f(one), f(true))" ->
      error("cannot unify types int and bool"),
    "fun f -> let x = dynamic_to_int(f) in pair(f(one), f(true))" ->
      error("cannot unify types int and bool"),
    "fun -> let f = fun x -> dynamic_to_int(x) in pair(f(one), f(true))" ->
      OK("() -> pair[int, int]"),
    "let f = (fun (x : ?) -> x)(one) in pair(f(one), f(true))" -> OK("pair[?, ?]"),
    "fun x -> eq(x, (fun (y : ?) -> y))" -> OK(" (? -> ?) -> bool"),
    "fun x y -> pair(eq(x, (fun (y : ?) -> y)), eq(x, y))" ->
      OK("(? -> ?, ? -> ?) -> pair[bool, bool]"),
    "fun x -> pair(eq(x, (fun (y : ?) -> y)), x(one))" -> OK("(int -> ?) -> pair[bool, ?]")
  )

  val test_dynamic_no_freeze = List[(String,Result)](
    "fun -> " +
     " let g = fun (x : _) -> dynamic_to_int(x) in " +
     " let y = g(one) in " +
     " g" -> OK("() -> int -> int"),
    "fun -> " +
     "(fun (g : _) -> " +
     "  (fun (y : _) -> g)(g(one))) " +
     " (fun (x : _) -> dynamic_to_int(x))" -> OK("() -> int -> int"),
    "let f = (fun (x : ?) -> x)(one) in pair(f(one), f(true))" ->
      error("cannot unify types int and bool"),
    "let f : ? -> ? = (fun (x : _) -> x) in pair(f(one), f(true))" ->
      error("cannot unify types int and bool")
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

  def make_single_test_case(settings: Settings, code:String, expected_result:Result) {
    val result = {
      try {
        Infer.reset_id()
        Infer.settings.dynamic_parameters = settings.dynamic_parameters
        Infer.settings.freeze_dynamic = settings.freeze_dynamic
        val ty = Infer.infer(Core.core, 0, parse.expr_eof(code))
        Infer.settings.freeze_dynamic = true
        val generalized_ty = Infer.generalize(-1, ty)
        OK(string_of_ty(generalized_ty))
      } catch {
        case e:Exception => Fail(Some(e.getMessage()))
      }
    }
    assert_equal(code, expected_result, result)
  }

  def apply() {
    test_hm_static.foreach{
      case(a, b) => make_single_test_case(Settings(false, true), a, b)
    }
    test_dynamic.foreach{
      case(a, b) => make_single_test_case(Settings(true, true), a, b)
    }
    test_dynamic_ann.foreach{
      case(a, b) => make_single_test_case(Settings(false, true), a, b)
    }
    test_dynamic_no_freeze.foreach{
      case(a, b) => make_single_test_case(Settings(true, false), a, b)
    }
  }
}

