package dhm

object Core {

  import Expr._
  import Infer._

  val core = Map(
        "head" -> "forall[a] list[a] -> a",
        "tail" -> "forall[a] list[a] -> list[a]",
        "nil" -> "forall[a] list[a]",
        "cons" -> "forall[a] (a, list[a]) -> list[a]",
        "cons_curry" -> "forall[a] a -> list[a] -> list[a]",
        "map" -> "forall[a b] (a -> b, list[a]) -> list[b]",
        "map_curry" -> "forall[a b] (a -> b) -> list[a] -> list[b]",
        "one" -> "int",
        "zero" -> "int",
        "succ" -> "int -> int",
        "plus" -> "(int, int) -> int",
        "eq" -> "forall[a] (a, a) -> bool",
        "eq_curry" -> "forall[a] a -> a -> bool",
        "not" -> "bool -> bool",
        "true" -> "bool",
        "false" -> "bool",
        "pair" -> "forall[a b] (a, b) -> pair[a, b]",
        "pair_curry" -> "forall[a b] a -> b -> pair[a, b]",
        "first" -> "forall[a b] pair[a, b] -> a",
        "second" -> "forall[a b] pair[a, b] -> b",
        "id" -> "forall[a] a -> a",
        "const" -> "forall[a b] a -> b -> a",
        "apply" -> "forall[a b] (a -> b, a) -> b",
        "apply_curry" -> "forall[a b] (a -> b) -> a -> b",
        "choose" -> "forall[a] (a, a) -> a",
        "choose_curry" -> "forall[a] a -> a -> a"
    ).map {
        case (key,ty_str) => (key,parse.ty_forall_eof(ty_str))
    }
}
