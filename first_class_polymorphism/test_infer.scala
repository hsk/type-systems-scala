package fcp
/*
  Parts of this file are based on code from Daan Leijen's
  reference implementation of HMF, available at (as of 2014/04/04)
  http://research.microsoft.com/en-us/um/people/daan/download/hmf-prototype-ref.zip
*/
object Test_infer {

  import Expr._

  sealed trait Result
  case class OK(a:String) extends Result
  case class Fail(a:Option[String]) extends Result

  val fail = Fail(None)
  def error(msg:String):Result = Fail(Some(msg))

  val test_cases = List[(String,Result)](
    // Hindley-Milner
    "id" -> OK("forall[a] a -> a"),
    "one" -> OK("int"),
    "x" -> error("variable x not found"),
    "let x = x in x" -> error("variable x not found"),
    "let x = id in x" -> OK("forall[a] a -> a"),
    "let x = fun y -> y in x" -> OK("forall[a] a -> a"),
    "fun x -> x" -> OK("forall[a] a -> a"),
    "pair" -> OK("forall[a b] (a, b) -> pair[a, b]"),
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
    "single(id)" -> OK("forall[a] list[a -> a]"),
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
    "let apply_curry = fun f -> fun x -> f(x) in apply_curry" -> OK("forall[a b] (a -> b) -> a -> b"),

    // HMF
    "ids" -> OK("list[forall[a] a -> a]"),
    "fun f -> pair(f(one), f(true))" -> fail,
    "fun (f : forall[a] a -> a) -> pair(f(one), f(true))" ->
      OK("(forall[a] a -> a) -> pair[int, bool]"),
    "cons(ids, nil)" -> OK("list[list[forall[a] a -> a]]"),
    "choose(nil, ids)" -> OK("list[forall[a] a -> a]"),
    "choose(ids, nil)" -> OK("list[forall[a] a -> a]"),
    "cons(fun x -> x, ids)" -> OK("list[forall[a] a -> a]"),
    "let rev_cons = fun x y -> cons(y, x) in rev_cons(ids, id)" -> OK("list[forall[a] a -> a]"),
    "cons(id, ids)" -> OK("list[forall[a] a -> a]"),
    "cons(id, cons(succ, nil))" -> OK("list[int -> int]"),
    "poly(id)" -> OK("pair[int, bool]"),
    "poly(fun x -> x)" -> OK("pair[int, bool]"),
    "poly(succ)" -> fail,
    "apply(succ, one)" -> OK("int"),
    "rev_apply(one, succ)" -> OK("int"),
    "apply(poly, id)" -> OK("pair[int, bool]"),
    "apply_curry(poly)(id)" -> OK("pair[int, bool]"),
    "rev_apply(id, poly)" -> OK("pair[int, bool]"),
    "rev_apply_curry(id)(poly)" -> fail,
    "(id : forall[a] a -> a) : int -> int" -> OK("int -> int"),
    "single(id : forall[a] a -> a)" -> OK("list[forall[a] a -> a]"),
    "(fun x -> fun y -> let z = choose(x, y) in z)(id : forall[a] a -> a)" ->
      OK("(forall[a] a -> a) -> (forall[a] a -> a)"),
    "fun (x : forall[a] a -> a) -> x" -> OK("forall[a] (forall[b] b -> b) -> a -> a"),
    "id_id(id)" -> OK("forall[a] a -> a"),
    "almost_id_id(id)" -> OK("forall[a] a -> a"),
    "fun id -> poly(id)" -> fail,
    "fun ids -> id_ids(ids)" -> fail,
    "poly(id(id))" -> OK("pair[int, bool]"),
    "length(ids)" -> OK("int"),
    "map(head, single(ids))" -> OK("list[forall[a] a -> a]"),
    "map_curry(head)(single(ids))" -> OK("list[forall[a] a -> a]"),
    "apply(map_curry(head), single(ids))" -> OK("list[forall[a] a -> a]"),
    "apply_curry(map_curry(head))(single(ids))" -> OK("list[forall[a] a -> a]"),
    "apply(id, one)" -> OK("int"),
    "apply_curry(id)(one)" -> OK("int"),
    "poly(magic)" -> OK("pair[int, bool]"),
    "id_magic(magic)" -> OK("forall[a b] a -> b"),
    "fun (f : forall[a b] a -> b) -> let a = id_magic(f) in one" -> OK("(forall[a b] a -> b) -> int"),
    "fun (f : some[a b] a -> b) -> id_magic(f)" -> fail,
    "id_magic(id)" -> fail,
    "fun (f : forall[a b] a -> b) -> f : forall[a] a -> a" ->
      OK("(forall[a b] a -> b) -> (forall[a] a -> a)"),
    "let const = (any : forall[a] a -> (forall[b] b -> a)) in const(any)" -> OK("forall[a b] a -> b"),

    // propagation of types
    "single(id) : list[forall[a] a -> a]" -> fail,
    "id(single(id)) : list[forall[a] a -> a]" -> fail,
    "cons(single(id), single(ids))" -> fail,
    "id_id(id) : int -> int" -> OK("int -> int"),
    "head(ids)(one) : int" -> OK("int"),
    "head(ids) : int -> int" -> OK("int -> int"),
    "let f = head(ids) in f : int -> int" -> OK("int -> int"),
    "cons(single(id) : list[forall[a] a -> a], single(single(fun x -> x)))" -> fail,
    "id_succ(head(map(id, ids)))" -> OK("int -> int"),
    "(fun f -> f(f)) : (forall[a] a -> a) -> (forall[a] a -> a)" -> fail,
    "(fun f -> f(f)) : forall[b] (forall[a] a -> a) -> b -> b" -> fail,
    "(let x = one in (fun f -> pair(f(x), f(true)))) : (forall[a] a -> a) -> pair[int, bool]" ->
      fail,
    "let returnST = any : forall[a s] a -> ST[s, a] in " +
     "returnST(one) : forall[s] ST[s, int]" -> OK("forall[s] ST[s, int]"),
    "special(fun f -> f(f))" -> fail,
    "apply(special, fun f -> f(f))" -> fail,
    "rev_apply(fun f -> f(f), special)" -> fail,
    "apply(fun f -> choose(id_id, f), id_id : (forall[a] a -> a) -> (forall[a] a -> a))" ->
      fail,
    "rev_apply(id_id : (forall[a] a -> a) -> (forall[a] a -> a), fun f -> choose(id_id, f))" ->
      fail
  )

  def string_of_result(r:Result):String = {
    r match {
      case Fail(None) => "Fail"
      case Fail(Some(msg)) => "Fail " + msg
      case OK(ty_str) => "OK " + ty_str
    }
  }

  def cmp_result(result1:Result, result2:Result):Boolean = {
    (result1, result2) match {
    case (Fail(None), Fail(_)) => true
    case (Fail(_), Fail(None)) => true
    case (Fail(Some(msg1)), Fail(Some(msg2))) => msg1 == msg2
    case (OK(ty_str1), OK(ty_str2)) => ty_str1 == ty_str2
    case _ => false
    }
  }

  def assert_equal(str:String, er:Result, r:Result) {
    if(!cmp_result(er,r))
      println("source "+str+" expected "+string_of_result(er)+" but result is "+string_of_result(r))

  }

  def make_single_test_case(code:String, expected_result:Result) {
    println("test " +code)
    val expected_result1 = expected_result match {
      case OK(ty_str) => OK(string_of_ty(Parse.ty_eof(ty_str)))
      case x => x
    }
    /*
    val result = {
      try {
        Infer.reset_id()
        val ty = Infer.infer(Core.core, 0, Parse.expr_eof(code))
        OK(string_of_ty(ty))
      } catch {
        case e:Exception => Fail(Some(e.getMessage()))
      }
    }
    assert_equal(code, expected_result1, result)*/
  }
  def apply() {
    test_cases.foreach{
      case(a, b) => make_single_test_case(a, b)
    }
  }
}
