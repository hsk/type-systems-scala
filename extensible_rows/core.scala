package dhm

object Core {

  import Expr._
  import Infer._

  val core = {
    val core_ref = Ref(Env.empty)

    def assume(name:String, ty_str:String) {
      val ty = parse.ty_forall_eof(ty_str)
      core_ref.a = Env.extend(core_ref.a, name, ty)
    }

    assume("head", "forall[a] list[a] -> a")
    assume("tail", "forall[a] list[a] -> list[a]")
    assume("nil", "forall[a] list[a]")
    assume("cons", "forall[a] (a, list[a]) -> list[a]")
    assume("cons_curry", "forall[a] a -> list[a] -> list[a]")
    assume("map", "forall[a b] (a -> b, list[a]) -> list[b]")
    assume("map_curry", "forall[a b] (a -> b) -> list[a] -> list[b]")
    assume("one", "int")
    assume("zero", "int")
    assume("succ", "int -> int")
    assume("plus", "(int, int) -> int")
    assume("eq", "forall[a] (a, a) -> bool")
    assume("eq_curry", "forall[a] a -> a -> bool")
    assume("not", "bool -> bool")
    assume("true", "bool")
    assume("false", "bool")
    assume("pair", "forall[a b] (a, b) -> pair[a, b]")
    assume("pair_curry", "forall[a b] a -> b -> pair[a, b]")
    assume("first", "forall[a b] pair[a, b] -> a")
    assume("second", "forall[a b] pair[a, b] -> b")
    assume("id", "forall[a] a -> a")
    assume("const", "forall[a b] a -> b -> a")
    assume("apply", "forall[a b] (a -> b, a) -> b")
    assume("apply_curry", "forall[a b] (a -> b) -> a -> b")
    assume("choose", "forall[a] (a, a) -> a")
    assume("choose_curry", "forall[a] a -> a -> a")

    core_ref.a
  }
}
