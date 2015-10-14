> <sup><sub>
Extensible records with scoped labels

# スコープドラベル付きの拡張レコード

> <sup><sub>
This is an implementation of type inference for safe, polymorphic and extensible records.

これは、安全で多相的な拡張可能なレコードの型推論の実装です。


> <sup><sub>
Overview

## 概要

<sup><sub>
considerably かなり
predicate 述語
lack 欠如
specify 指定
presentation 発表、提示
relatively 比較的
</sub></sup>

> <sup><sub>
In his paper [Extensible records with scoped labels][1], Daan Leijen describes an innovative
type inference system for extensible records which allows duplicate labels in rows. This makes
it considerably simpler than most other record systems, which include predicates on record
types such as the "lacks" predicate *`(r\l)`*, specifying that the record type `r` must not
contain label `l`. This implementation closely follows Daan's presentation in his paper and
is a relatively small extension of the Hindley-Milner type inference algorithm implemented
in **algorithm_w** (the changes can be seen in commit [5c183a7][2]).

Daan Leijenは彼の論文[スコープドラベル付き拡張レコード][1]で、列に重複するラベルを可能にする拡張可能なレコードのための革新的な型推論システムを説明しています。
これは、レコードの型 `r` がラベル `l` を含んではならないことを指定して、"欠けている" 述語 *`(r/l)`* などのレコードタイプの述語が含まれるような他のほとんどのレコードシステムよりもそれはかなり簡単になります。
この実装は、密接に彼の論文でDaanののプレゼンテーションに続き、algorithm_wに実装ヒンドリー - ミルナー型推論アルゴリズムの比較的小さい拡張したものです（変更はコミット[5c183a7][2]で見ることができます）。

<sup><sub>
consist 構成する
restriction 制限
</sub></sup>

> <sup><sub>
Records consist of labeled fields with values `{a = one, b = false}` and can extend other
records `{x = false | r}`. The basic operations for records are *selection*, *extension*
and *restriction* and are typed as follows:

レコードの値を持つラベルのフィールドで構成`{a = one, b = false}`とその他のレコードを拡張することができます。`{x = false | r}`。
レコードのための基本的な操作は、選択、拡張および制限され、次のように型定義されます：

```
	(_.label) : forall[a r] {label : a | r} -> a
	{label = _ | _} : forall[a r] (a, {r}) -> {label : a | r}
	{_ - label} : forall[a r] {label : a | r} -> {r}
```



> <sup><sub>
Details

## 詳細

<sup><sub>
either どちらか
Syntax sugar 構文糖
consist 構成される
wrapper ラッパー
</sub></sup>

> <sup><sub>
The types of expressions `expr` and types `ty` in `expr.ml` are extended with primitive record operations and types.
Records can either be empty `{}` or extensions of other records `{x = false | r}`.
Syntax sugar for `{x = false | {y = zero | {}}}` is `{x = false, y = zero}`.
The type of rows similarly consists of empty rows `<>` and row extensions `<a : _ | ...>`.
A record type is a wrapper for the type of row; other wrappers could exist (Daan gives example of sum/variant types).

<sup><sub>
enclosing 囲む
handled 扱う、取り扱う
</sub></sup>

> <sup><sub>
The core of the type inference is implemented in functions `unify` and `rewrite_row`.
The function `unify` unifies record types by unifying their enclosing rows, and unifies an empty row only with itself.
If a row extension `<a : t | r>` is unified with another row, the function `rewrite_row` rewrites the second row by searching for the first field with label `a` and unifies its type with `t`.
All other types are handled as before.

<sup><sub>
significant 重要な
above 上記
restriction 制限
</sub></sup>

> <sup><sub>
The only other significant change is in function `infer`, where the types of new expression terms are inferred by treating them as implicit calls to *selection*, *extension* and *restriction* functions with types as above.


> <sup><sub>
Discussion

## 考察

<sup><sub>
potential ポテンシャル、潜在的、可能性
represented 表す、表現
whose その、持つ
procedure 手続き
rearrange 再編成、並べ替え、再配置
necessary 必要
canonically 標準的に
gather 収集、集まる
</sub></sup>

> <sup><sub>
One potential problem with this implementation is that record literals and row types are represented as a list of record/row extensions, whose order depends on programmer's code and inner workings of the type inference algorithm.
The unification procedure can rearrange fields as necessary, but records and record types can not be easily compared or canonically represented by strings.
A better solution would be to gather all labels into a multi-map and use a specific sorting order for labels when representing rows as strings (implemented in [**extensible_rows2**][5]).

<sup><sub>
While ながら、している間
possibility 可能性
proposals 提案
summarized 要約、まとめる
predicate 述語
disjoint 互いに素、バラバラ
structural subtyping 構造的部分型付け
</sub></sup>

> <sup><sub>
While this type system is simple to implement and use (for example, it is a part of the language [Elm][3]), it represents only one possibility for typing extensible records.
Other proposals, summarized in [GHC wiki][4], include first-class labels, positive and negative ("lacks") predicates for record types and even more general predicates such as "disjoint", and also include structural subtyping (as used for objects in OCaml and Go).

[1]: http://research.microsoft.com/apps/pubs/default.aspx?id=65409
[2]: https://github.com/tomprimozic/type-systems/commit/5c183a7866aa30f3350a4cab011e376d36dd385e
[3]: http://elm-lang.org/learn/Records.elm
[4]: https://ghc.haskell.org/trac/ghc/wiki/ExtensibleRecords
[5]: https://github.com/tomprimozic/type-systems/tree/master/extensible_rows2

----

# extensible_rows

extensible\_rowsはalgorithm\_wにレコードの機能を拡張したものです。

extensible\_rowsの拡張は以下のテストを通るようにします。

## diff test_parser.scala

    // records
    "{}" -> OK(RecordEmpty),
    "{ }" -> OK(RecordEmpty),
    "{" -> Fail,
    "a.x" -> OK(RecordSelect(Var("a"), "x")),
    "{m - a}" -> OK(RecordRestrict(Var("m"), "a")),
    "{m - a" -> Fail,
    "m - a" -> Fail,
    "{a = x}" -> OK(RecordExtend("a", Var("x"), RecordEmpty)),
    "{a = x" -> Fail,
    "{a=x, b = y}" -> OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), RecordEmpty))),
    "{b = y ,a=x}" -> OK(RecordExtend("b", Var("y"), RecordExtend("a", Var("x"), RecordEmpty))),
    "{a=x,h=w,d=y,b=q,g=z,c=t,e=s,f=r}" ->
      OK(RecordExtend("a", Var("x"), RecordExtend("h", Var("w"), RecordExtend("d", Var("y"),
        RecordExtend("b", Var("q"), RecordExtend("g", Var("z"), RecordExtend("c", Var("t"),
        RecordExtend("e", Var("s"), RecordExtend("f", Var("r"), RecordEmpty))))))))),
    "{a = x|m}" -> OK(RecordExtend("a", Var("x"), Var("m"))),
    "{a | m}" -> Fail,
    "{ a = x, b = y | m}" -> OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), Var("m")))),
    "{ a = x, b = y | {m - a} }" ->
      OK(RecordExtend("a", Var("x"), RecordExtend("b", Var("y"), RecordRestrict(Var("m"), "a")))),
    "{ b = y | m - a }" -> Fail,
    "let x = {a = f(x), b = y.b} in { a = fun z -> z | {x - a} }" ->
      OK(Let("x", RecordExtend("a", Call(Var("f"), List(Var("x"))), RecordExtend("b",
        RecordSelect(Var("y"), "b"), RecordEmpty)), RecordExtend("a", Fun(List("z"), Var("z")),
        RecordRestrict(Var("x"), "a"))))


## diff test_infer.scala

    // records
    "{}" -> OK("{}"),
    "{}.x" -> fail,
    "{a = one}" -> OK("{a : int}"),
    "{a = one, b = true}" -> OK("{a : int, b : bool}"),
    "{b = true, a = one}" -> OK("{b : bool, a : int}"),
    "{a = one, b = true}.a" -> OK("int"),
    "{a = one, b = true}.b" -> OK("bool"),
    "{a = one, b = true}.c" -> error("row does not contain label c"),
    "{f = fun x -> x}" -> OK("forall[a] {f : a -> a}"),
    "let r = {a = id, b = succ} in choose(r.a, r.b)" -> OK("int -> int"),
    "let r = {a = id, b = fun x -> x} in choose(r.a, r.b)" -> OK("forall[a] a -> a"),
    "choose({a = one}, {})" -> fail,
    "{ x = zero | { y = one | {} } }" -> OK("{x : int, y : int}"),
    "choose({ x = zero | { y = one | {} } }, {x = one, y = zero})" -> OK("{x : int, y : int}"),
    "{{} - x}" -> fail,
    "{{x = one, y = zero} - x}" -> OK("{y : int}"),
    "{ x = true | {x = one}}" -> OK("{x : bool, x : int}"),
    "let a = {} in {b = one | a}" -> OK("{b : int}"),
    "let a = {x = one} in {x = true | a}.x" -> OK("bool"),
    "let a = {x = one} in a.y" -> error("row does not contain label y"),
    "let a = {x = one} in {a - x}" -> OK("{}"),
    "let a = {x = one} in let b = {x = true | a} in {b - x}.x" -> OK("int"),
    "fun r -> {x = one | r}" -> OK("forall[r] {r} -> {x : int | r}"),
    "fun r -> r.x" -> OK("forall[r a] {x : a | r} -> a"),
    "let get_x = fun r -> r.x in get_x({y = one, x = zero})" -> OK("int"),
    "let get_x = fun r -> r.x in get_x({y = one, z = true})" -> error("row does not contain label x"),
    "fun r -> choose({x = zero | r}, {x = one | {}})" -> OK("{} -> {x : int}"),
    "fun r -> choose({x = zero | r}, {x = one})" -> OK("{} -> {x : int}"),
    "fun r -> choose({x = zero | r}, {x = one | r})" -> OK("forall[r] {r} -> {x : int | r}"),
    "fun r -> choose({x = zero | r}, {y = one | r})" -> error("recursive row types"),
    "let f = fun x -> x.t(one) in f({t = succ})" -> OK("int"),
    "let f = fun x -> x.t(one) in f({t = id})" -> OK("int"),
    "{x = one, x = true}" -> OK("{x : int, x : bool}"),
    "let f = fun r -> let y = r.y in choose(r, {x = one, x = true}) in f" ->
      error("row does not contain label y"),
    "fun r -> let y = choose(r.x, one) in let z = choose({r - x}.x, true) in r" ->
      OK("forall[a r] {x : int, x : bool | r} -> {x : int, x : bool | r}"),
    "fun r -> choose({x = zero | r}, {x = one, x = true})" -> OK("{x : bool} -> {x : int, x : bool}"),
    "fun r -> choose(r, {x = one, x = true})" -> OK("{x : int, x : bool} -> {x : int, x : bool}"),
    "fun r -> choose({x = zero | r}, {x = true | r})" -> error("cannot unify types int and bool")

## diff expr.ml

追加された構文木

	  case class RecordSelect(a:Expr, b: name) extends Expr          // selecting value of label: `r.a`
	  case class RecordExtend(a:name, b: Expr, c: Expr) extends Expr // extending a record: `{a = 1 | r}`
	  case class RecordRestrict(a: Expr, b: name) extends Expr       // deleting a label: `{r - a}`
	  case object RecordEmpty extends Expr                           // empty record: `{}`

型

	  case class TRecord(a:Row) extends Ty                  // record type: `{<...>}`
	  case object TRowEmpty extends Ty                      // empty row: `<>`
	  case class TRowExtend(a:name, b:Ty, c:Row) extends Ty // row extension: `<a : _ | ...>`
	  type Row = Ty // the kind of rows - empty row, row variable, or row extension

構文木の印字

      case RecordEmpty => "{}"
      case RecordSelect(record_expr, label) => f(true, record_expr) + "." + label
      case RecordRestrict(record_expr, label) => "{" + f(false, record_expr) + " - " + label + "}"
      case RecordExtend(label, expr, record_expr) =>
          def g(str:String, e:Expr):String = {
            e match {
            case RecordEmpty => str
            case RecordExtend(label, expr, record_expr) =>
              g(str + ", " + label + " = " + f(false, expr), record_expr)
            case other_expr => str + " | " + f(false, other_expr)
            }
          }
          "{" + g(label + " = " + f(false, expr), record_expr) + "}"


型の印字

        case TRecord(row_ty) => "{" + f(false, row_ty) + "}"
        case TRowEmpty => ""
        case TRowExtend(label, ty, row_ty) =>
          def g(str: String, t:Ty): String = {
            t match {
              case TRowEmpty => str
              case TRowExtend(label, ty, row_ty) =>
                g(str + ", " + label + " : " + f(false, ty), row_ty)
              case TVar(Ref(Link(ty))) => g(str, ty)
              case other_ty => str + " | " + f(false, other_ty)
            }
          }
          g(label + " : " + f(false, ty), row_ty)


## infer.ml


型推論

        case TRecord(row) => f(row)
        case TRowExtend(label, field_ty, row) => f(field_ty); f(row)
        case TRowEmpty =>

TODO:

> 		| TRecord row1, TRecord row2 -> unify row1 row2
> 		| TRowEmpty, TRowEmpty -> ()
> 		| TRowExtend(label1, field_ty1, rest_row1), (TRowExtend _ as row2) -> begin
> 				let rest_row1_tvar_ref_option = match rest_row1 with
> 					| TVar ({contents = Unbound _} as tvar_ref) -> Some tvar_ref
> 					| _ -> None
> 				in
> 				let rest_row2 = rewrite_row row2 label1 field_ty1 in
> 				begin match rest_row1_tvar_ref_option with
> 					| Some {contents = Link _} -> error "recursive row types"
> 					| _ -> ()
> 				end ;
> 				unify rest_row1 rest_row2
> 			end


> and rewrite_row row2 label1 field_ty1 = match row2 with
> 	| TRowEmpty -> error ("row does not contain label " ^ label1)
> 	| TRowExtend(label2, field_ty2, rest_row2) when label2 = label1 ->
> 			unify field_ty1 field_ty2 ;
> 			rest_row2
> 	| TRowExtend(label2, field_ty2, rest_row2) ->
> 			TRowExtend(label2, field_ty2, rewrite_row rest_row2 label1 field_ty1)
> 	| TVar {contents = Link row2} -> rewrite_row row2 label1 field_ty1
> 	| TVar ({contents = Unbound(id, level)} as tvar) ->
> 			let rest_row2 = new_var level in
> 			let ty2 = TRowExtend(label1, field_ty1, rest_row2) in
> 			tvar := Link ty2 ;
> 			rest_row2
> 	| _ -> error "row type expected"
> 


< 	| TVar {contents = Generic _} | TVar {contents = Unbound _} | TConst _ as ty -> ty
---
> 	| TRecord row -> TRecord (generalize level row)
> 	| TRowExtend(label, field_ty, row) ->
> 			TRowExtend(label, generalize level field_ty, generalize level row)
> 	| TVar {contents = Generic _} | TVar {contents = Unbound _} | TConst _ | TRowEmpty as ty -> ty


> 		| TRecord row -> TRecord (f row)
> 		| TRowEmpty -> ty
> 		| TRowExtend(label, field_ty, row) ->
> 				TRowExtend(label, f field_ty, f row)


> 	| RecordEmpty -> TRecord TRowEmpty
> 	| RecordSelect(record_expr, label) ->
> 			(* inlined code for Call of function with type "forall[a r] {label : a | r} -> a" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			let return_ty = field_ty in
> 			unify param_ty (infer env level record_expr) ;
> 			return_ty
> 	| RecordRestrict(record_expr, label) ->
> 			(* inlined code for Call of function with type "forall[a r] {label : a | r} -> {r}" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			let return_ty = TRecord rest_row_ty in
> 			unify param_ty (infer env level record_expr) ;
> 			return_ty
> 	| RecordExtend(label, expr, record_expr) ->
> 			(* inlined code for Call of function with type "forall[a r] (a, {r}) -> {label : a | r}" *)
> 			let rest_row_ty = new_var level in
> 			let field_ty = new_var level in
> 			let param1_ty = field_ty in
> 			let param2_ty = TRecord rest_row_ty in
> 			let return_ty = TRecord (TRowExtend(label, field_ty, rest_row_ty)) in
> 			unify param1_ty (infer env level expr) ;
> 			unify param2_ty (infer env level record_expr) ;
> 			return_ty

## parser.mly


> let expr_record_extend label_expr_list record =
> 	List.fold_left
> 		(fun record (label, expr) -> RecordExtend(label, expr, record))
> 		record label_expr_list
> 
> let ty_row_extend label_ty_list row =
> 	List.fold_left
> 		(fun row (label, ty) -> TRowExtend(label, ty, row))
> 		row label_ty_list
> 


< 		| TApp(ty, ty_arg_list) ->
< 				TApp(f ty, List.map f ty_arg_list)
< 		| TArrow(param_ty_list, return_ty) ->
< 				TArrow(List.map f param_ty_list, f return_ty)
---
> 		| TApp(ty, ty_arg_list) -> TApp(f ty, List.map f ty_arg_list)
> 		| TArrow(param_ty_list, return_ty) -> TArrow(List.map f param_ty_list, f return_ty)
> 		| TRecord row -> TRecord (f row)
> 		| TRowEmpty -> ty
> 		| TRowExtend(label, ty, row) -> TRowExtend(label, f ty, f row)



> 	| LBRACE RBRACE                                     { RecordEmpty }
> 	| LBRACE record_label_expr_list PIPE expr RBRACE    { expr_record_extend $2 $4 }
> 	| LBRACE record_label_expr_list RBRACE              { expr_record_extend $2 RecordEmpty }
> 	| LBRACE expr MINUS IDENT RBRACE                    { RecordRestrict($2, $4) }
> 	| simple_expr DOT IDENT                             { RecordSelect($1, $3) }


> record_label_expr_list:
> 	| IDENT EQUALS expr                               { [($1, $3)] }
> 	| record_label_expr_list COMMA IDENT EQUALS expr  { ($3, $5) :: $1 }
> 


> 	| LBRACE RBRACE                                 { TRecord TRowEmpty }
> 	| LBRACE IDENT RBRACE                           { TRecord (TConst $2) }
> 	| LBRACE ty_row PIPE ty RBRACE                  { TRecord (ty_row_extend $2 $4) }
> 	| LBRACE ty_row RBRACE	                        { TRecord (ty_row_extend $2 TRowEmpty) }


> ty_row:
> 	| IDENT COLON ty                    { [($1, $3)] }
> 	| ty_row COMMA IDENT COLON ty       { ($3, $5) :: $1 }


## EBNF

    ident                   ::= [_A-Za-z][_A-Za-z0-9]*
    integer                 ::= [0-9]+

    expr                    ::= simple_expr rep(app_rep)
                              | "let" ident "=" expr "in" expr
                              | "fun" rep1(ident) "->" expr                                   

    record_label_expr_list  ::= rep1sep(ident "=" expr, ",")

    app_rep                 ::= "(" rep1sep(expr, ",") ")"
                              | "." ident

    simple_expr             ::= ident
                              | "(" expr ")"
                              | "{" "}"
                              | "{" record_label_expr_list "|" expr "}"
                              | "{" record_label_expr_list "}"
                              | "{" expr "-" ident "}"

    ty_forall               ::= ty
                              | "forall" "[" rep1(ident) "]" ty

    ty:Parser[Ty]           ::= app_ty "->" ty
                              | app_ty
                              | "(" repsep(ty, ",") ")" "->" ty

    ty_row                  ::= rep1sep(ident ":" ty, ",")

    app_ty                  ::= simple_ty rep("[" rep1sep(ty, ",") "]")

    simple_ty               ::= ident
                              | "(" ty ")"
                              | "{" "}"
                              | "{" ident "}"
                              | "{" ty_row "|" ty "}"
                              | "{" ty_row "}"
