package fcp

object Core {

  import Expr._
  import Infer._
  
  val core = {
    val declaration_list = List(
      "head : forall[a] list[a] -> a",
      "tail : forall[a] list[a] -> list[a]",
      "nil : forall[a] list[a]",
      "single : forall[a] a -> list[a]",
      "cons : forall[a] (a, list[a]) -> list[a]",
      "cons_curry : forall[a] a -> list[a] -> list[a]",
      "map : forall[a b] (a -> b, list[a]) -> list[b]",
      "map_curry : forall[a b] (a -> b) -> list[a] -> list[b]",
      "length : forall[a] list[a] -> int",

      "one : int",
      "zero : int",
      "succ : int -> int",
      "plus : (int, int) -> int",
      "eq : forall[a] (a, a) -> bool",
      "eq_curry : forall[a] a -> a -> bool",
      "not : bool -> bool",
      "true : bool",
      "false : bool",

      "pair : forall[a b] (a, b) -> pair[a, b]",
      "pair_curry : forall[a b] a -> b -> pair[a, b]",
      "first : forall[a b] pair[a, b] -> a",
      "second : forall[a b] pair[a, b] -> b",

      "const : forall[a b] a -> b -> a",
      "apply : forall[a b] (a -> b, a) -> b",
      "apply_curry : forall[a b] (a -> b) -> a -> b",
      "rev_apply : forall[a b] (a, a -> b) -> b",
      "rev_apply_curry : forall[a b] a -> (a -> b) -> b",
      "choose : forall[a] (a, a) -> a",
      "choose_curry : forall[a] a -> a -> a",

      "magic : forall[a b] a -> b",
      "any : forall[a] a",
      "poly : (forall[a] a -> a) -> pair[int, bool]",

      "id : forall[a] a -> a",
      "ids : list[forall[a] a -> a]",
      "id_id : (forall[a] a -> a) -> (forall[a] a -> a)",
      "almost_id_id : forall[a] (forall[a] a -> a) -> a -> a",
      "id_ids : list[forall[a] a -> a] -> list[forall[a] a -> a]",
      "id_magic : (forall[a b] a -> b) -> (forall[a b] a -> b)",
      "id_succ : (int -> int) -> (int -> int)",
      "special : ((forall[a] a -> a) -> (forall[a] a -> a)) -> (forall[a] a -> a)"
    )
        
    declaration_list.foldLeft(Env.empty) {
      case (env, declaration_str) =>
        val expr = Parse.expr_eof(declaration_str)
        expr match {
          case Ann(Var(name), (List(), ty)) => Env.extend(env, name, ty)
          case _ => throw new Exception("expected a variable annotated by a complete type")
        }
    }
  }
  
}

