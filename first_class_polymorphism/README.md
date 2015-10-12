TODO:

- [ ] テストを終わらせる
- [x] 適当に翻訳する
- [ ] 翻訳をよりよく修正する

First-class polymorphism
========================

|English|日本語|
|intuitive|直感的な|
|manner|やり方、方法|


This in an extension of ordinary Damas-Hindley-Milner type inference that supports first-class (impredicative) and higher-rank polymorphism in a very simple and intuitive manner, requiring only minimal type annotations.

これは、従来のDamas-Hindley-Milner型推論を非常にシンプルかつ直感的な方法で最小限の型注釈が必要なファーストクラス（非可述的）と高階の多相型をサポートして拡張したものです。

Overview
--------

概要
-----

|English|日本語|
| --- | --- |
|quite|非常に|
|limited|限られた|
|neither|どちらでもない|
|undecidable|否定出来ない、申し分ない|
|complicated|複雑な|
|capable|能力のある, 可能な, 有能な|


Although classical Damas-Hindley-Milner type inference supports polymorphic types, polymorphism is quite limited: function parameters cannot have polymorphic types and neither can type variables be instantiated with them.
On the other hand we have [System F][system_f], which supports all of the above, but for which type inference is undecidable.
In 2003, Didier Le Botlan and Didier Rémy presented [ML<sup>F</sup>][mlf], a system capable of inferring polymorphic types with minimal type annotations (only function parameters used polymorphically need to be annotated), which however uses complicated bounded types and even more complicated type inference algorithm.

古典的なダマ・ヒンドリー - ミルナーの型推論が多相的な型をサポートしていますが、多相性は非常に限られています：関数のパラメータは、多相的な型を持つことができないと、どちらも型変数にそれらを使用してインスタンス化することはできません。
一方、上記のすべてがサポートされている[System F][system_f]はありますが、型推論は決定不能です。
2003年にディディエルBotlanとディディエ・レミーによって発表された[ML<sup>F</sup>][mlf]は最小限の型注釈を持つ多相型を推論することができるシステムですが、複雑な有界型とより複雑な型推論アルゴリズムを使用しています。

|English|日本語|
| --- | --- |
|contrast|コントラスト、対比|
|intuitive|直感的な|
|considerably|かなり|
|since|なぜなら〜だからだ|
|least|最小|
|proposed|提案された|
|n-ary|n項の|
|rigid|固い|
|significantly|かなり，著しく，はっきりと|
|increase|増加，増大，増進，増殖|
|expressive|表現的な|
|improve|進歩させる|
|practical|実用的な|

This implementation is based on the work of Daan Leijen, published in his paper [HMF: Simple Type Inference for First-Class Polymorphism][hmf].
In contrast to ML<sup>F</sup>, it only uses very intuitive System F types and has a considerably simpler type inference algorithm, yet it requires more type annotations, since it always infers the least polymorphic type.
In addition to the basic algorithm, this implementation includes some extensions proposed by Daan in his paper, including rigid annotations and support for n-ary function calls, which can significantly increase HMF's expressive power and improve its practical usability.

この実装は Daan Leijen の仕事に基づいており、彼の論文に発表されました[HMF: ファーストクラス多相型のためのシンプルな型推論][hmf]。
ML<sup>F</sup>とは対照的に、HMFは非常に直感的なSystem Fの型を使用しており、かなり単純な型推論アルゴリズムですが、まだそれは常に少なくとも多相的な型を推論するので、HMFは、より多くの型注釈が必要です。
基本的なアルゴリズムに加えて、この実装では、彼の論文でDaanにより提案されたいくつかの拡張を含んでいて、n項の関数呼び出しのためのrigitアノテーションやサポートなど、かなりHMFの表現力を向上させ、その実用性を向上させることができます。


Features supported by HMF:

HMFがサポートしている機能:

  - *polymorphic type parameters* 多相的型引数

    |English|日本語|
    | --- | --- |
    |indeed|たしかに、まあ，さよう、いかにも|

    Parameters used polymorphically require type annotations even in ML<sup>F</sup>; indeed, without the type annotation this would more likely be programmer error.

    確かに多相的に使用されるパラメータも、ML<sup>F</sup>で型注釈が必要ですが、型注釈なしに、これは可能性が高いプログラマエラーになります。


    ```
    let poly = fun (f : forall[a] a -> a) -> pair(f(one), f(true)) in
    poly : (forall[a] a -> a) -> pair[int, bool]
    ```

    Parameters *not* used polymorphically, but only passed on to other functions as
    polymorphic arguments, need to be annotated (unlike in ML<sup>F</sup>).

    パラメータは、多相的に使用されていませんが、唯一の多相引数として他の関数に渡され、（MLFと異なり）注釈を付けることが必要です。


    ```
    let f = fun (x : forall[a] a -> a) -> poly(x) in
    f : (forall[a] a -> a) -> pair[int, bool]
    ```

  - *impredicative polymorphism* 非可述的多相性

    |English|日本語|
    | --- | --- |
    |During|間に、中に|
    |below|以下、次|
    |correct|正しい|

    Type variables such as `a` and `b` in `(a -> b, a) -> b` can be instantiated to
    polymorphic type. During type checking of the example below, the type variable `a` is
    instantiated to a polymorphic type `forall[c] c -> c`, and the correct type is inferred.

    `(a -> b, a) -> b` の `a` と `b` のような型変数は多相型にインスタンス化することができます。
    以下の例の型チェック時には、型変数`a`は、多相型`forall[c] c -> c`にインスタンス化され、正しい型が推論されます。

    ```
    let apply = fun f x -> f(x) in
    apply : forall[a b] (a -> b, a) -> b

    apply(poly, id) : pair[int, bool]
    ```

  - *rigid type annotations* 厳格な型注釈


    |English|日本語|
    | --- | --- |
    |absence|欠席|
    |least|最低、最小|
    |specify|特定する|

    In absence of type annotations, HMF will always infer the least polymorphic type.

    型注釈がない場合には、HMFは常に少なくとも多相的な型を推論します。

    ```
    let single = fun x -> cons(x, nil) in
    single : forall[a] a -> list[a]

    let ids = single(id) in
    ids : forall[a] list[a -> a]
    ```

    The programmer can specify a more polymorphic type for `ids` using type annotations.
    プログラマは、型注釈を使用して`ids`にさらに多相的な型を指定できます。

    ```
    let ids = single(id : forall[a] a -> a) in
    ids : list[forall[a] a -> a]
    ```


Details
-------

詳細
-------

|English|日本語|
| --- | --- |
|represents|表す|
|quantifiers|数量子|
|unspecified|不特定、未定義|

Types and expressions of HMF are simple extensions of what we had in `algorithm_w`.
We add the `TForall` constructor, which enables us to construct polymorphic types, and now have 3 types of type variables: `Unbound` type variables can be unified with any other type, `Bound` represents those variables bound by `forall` quantifiers or type annotations, and `Generic` type variables help us typecheck polymorphic parameters and can only be unified with themselves. 
Expressions are extended with *type annotations* `expr : type`.
Type annotations can bind additional unspecified type variables, and can also appear on function parameters:

HMFの型と式は、我々が `algorithm_w` で用いていた物を単純に拡張したものです。
我々は、多相的な型を構築する`TForall`コンストラクタを追加し、
現在、型変数の3つの型があり:
`Unbound`型変数は、他の型に単一化することができ、
`Bound`は` forall`量指定子または型注釈によって結合されたこれらの変数を表し、
`Generic`型の変数は、型チェック時に多相的なパラメータで私たちを助け、自分自身だけで単一化することができます。
式は *型注釈* `expr : type` で拡張されています。
型注釈が追加指定されていない型変数をバインドすることができ、また、関数のパラメータに表示されることがあります:

```
let f_one = fun f -> f(one) in
f_one : forall[a] (int -> a) -> a

let f_one_ann = fun (f : some[a] a -> a) -> f(one) in
f_one_ann : (int -> int) -> int
```

|English|日本語|
| --- | --- |
|important|重要|
|canonical|標準的な|
|efficiently|効果的に|


An important part of the type inference is the `replace_ty_constants_with_vars` function in `parser.mly`, which turns type constants `a` and `b` into `Bound` type variables if they are bound by `forall[a]` or `some[b]`.
This function also *normalizes* the type, ordering the bound variables in order in which they appear in the structure of the type, and removing unused ones.
This turns different versions of the same type `forall[b a] a -> b` and `forall[c a b] a -> b` into a canonical representation `forall[a b] a -> b`, allowing the type inference engine to efficiently unify polymorphic types.

型推論の重要な部分は`parser.mly`内の`replace_ty_constants_with_vars`関数で、`forall[a]`または`some[b]`によって束縛されている場合に型定数`a`と`Bound`型変数内の`b`に変えます。
この機能は、型を*正規化*し、型の構造に表示されている順にバインドされた変数を順序付け、未使用のものを除去します。
これは、同じ型の`forall[b a] a -> b`と`forall[c a b] a -> b`の異なるバージョンの標準的な表現`forall[a b] a -> b`に変わり、型推論エンジンが効率的に多相型を単一化することができます。

|English|日本語|
| --- | --- |
|respective|それぞれの|
|appear|現れる、表示する|
|determined|決定|

Function `substitute_bound_vars` in `infer.ml` takes a list of `Bound` variable ids, a list of replacement types and a type and returns a new type with `Bound` variables substituted with respective replacement types.
Function `escape_check` takes a list of `Generic` type variables and types `ty1` and `ty2` and checks if any of the `Generic` type variables appears in any of the sets of free generic variables in `ty1` or `ty2`, which are determined using the function `free_generic_vars`.

`infer.ml`内の関数`substitute_bound_vars`は `Bound`変数のidのリストと、置換型リストと、型をとり、それぞれの交換タイプで置換した`Bound`変数を使用して新しい型を返します。
`escape_check`関数は`Generic`型変数のリストと型`ty1`と`ty2`を取り、
`Generic`型変数リスト内の要素が`ty1`か`ty2`のフリーで一般的な変数のセット(これは、関数 `free_generic_vars`を使用して決定されます)内にあるかをチェックします。

|English|日本語|
| --- | --- |
|rely|頼る|
|equivalent|等価、同等の|
|substitute|代入|
|otherwise|一方|

The main difference in function `unify` is the unification of polymorphic types.
First, we create a fresh `Generic` type variable for every type variable bound by the two polymorphic types.
Here, we rely on the fact that both types are normalized, so equivalent generic type variables should appear in the same locations in both types.
Then, we substitute all `Bound` type variables in both types with `Generic` type variables, and try to unify them.
If unification succeeds, we check that none of the `Generic` type variables "escapes", otherwise we would successfully unify types `forall[a] a -> a` and `forall[a] a -> b`, where `b` is a unifiable `Unbound` type variable.

関数 `unify`の主な違いは、多相的な型の単一化です。
まず、2つの多相型によって結合されたすべての型変数のための新鮮な`Generic`型変数を作成します。
ここで、我々は両方の型が正規化されているという事実に依存しているため、同等のジェネリック型変数は、両方の型で同じ位置に現われる必要があります。
その後、我々は`Generic`型の変数と両方の型のすべての`Bound`型変数を代入し、それらを単一化してみてください。
単一化に成功した場合、我々は`Generic`型変数のいずれも「エスケープしない」ことを確認し、そうでない場合我々が正常に`b`が単一化可能な`Unbound`型変数である型`forall[a] a -> a` と `forall[a] a -> b`を、単一化します。

Function `instantiate` instantiates a `forall` type by substituting bound type variables by fresh `Unbound` type variables, which can then by unified with any other type.
Function `instantiate_ty_ann` does the same for type annotations.
The function `generalize` transforms a type into a `forall` type by substituting all `Unbound` type variables with levels higher than the input level with `Bound` type variables.
It traverses the structure of the types in a depth-first, left-to-right order, same as the function `replace_ty_constants_with_vars`, making sure that the resulting type is in normal form.

関数 `instantiate` は、新鮮な `Unbound` 型変数によって結合された型変数を代入して `forall` 型をインスタンス化することで、他のタイプで単一化することができます。
関数 `instantiate_ty_ann` は、型注釈のために同じことを行います。
関数 `generalize` は、`Bound` 型変数を使って入力レベルよりも高いレベルですべての `Unbound`型変数を代入することにより、 `forall` 型に型変換します。
これは、深さ優先の型の構造を移動し、左から右の順、関数 `replace_ty_constants_with_vars`と同じで、結果の型は正規形であることを確認します。

|English|日本語|
| --- | --- |
|subsume|包含する|

The function `subsume` takes two types `ty1` and `ty2` and determines if `ty1` is an *instance* of `ty2`.
For example, `int -> int` is an instance of `forall[a] a -> a` (the type of `id`), which in turn is an instance of `forall[a b] a -> b` (the type of `magic`).
This means that we can pass `id` as an argument to a function expecting `int -> int` and we can pass `magic` to a function expecting `forall[a] a -> a`, but not the other way round.
To determine if `ty1` is an instance of `ty2`, `subsume` first instantiates `ty2`, the more general type, with `Unbound` type variables.
If `ty1` is not polymorphic, is simply unifies the two types.
Otherwise, it instantiates `ty1` with `Generic` type variables and unifies both instantiated types.
If unification succeeds, we check that no generic variable escapes, same as in `unify`.

関数 `subsume` は、2つの型 `ty1` と `ty2` を取り、`ty1` は `ty2`の*インスタンス*であるかを判別します。
例えば、`int -> int` は `forall[a] a -> a` (`id`の型)のインスタンスで、ひいては`forall[a b] a -> b`(`magic`の型)のインスタンスです。
これは、我々は関数予期される `int -> int` への引数として `id` を渡すことができ、我々は `forall[a] a -> a` 予期関数に順番を逆にしないで `magic` を渡すことができることを意味します。
`ty1`は`ty2`のインスタンスであるかどうかを判断するには、`subsume`は最初`Unbound`型変数を`ty2`、より一般的な型である、インスタンス化します。
`ty1`が多相型でない場合は、単に2つの型を単一化します。
それ以外の場合は、`Generic`型変数で` ty1`をインスタンス化し、インスタンス化された型の両方を単一化します。
単一化に成功した場合、我々は`unify`と同じで、全く一般的な変数はエスケープしないことを確認してください。

|English|日本語|
| --- | --- |
|significantly|大幅に|
|instead|それよりも、かえって|
|might be|かもしれません|

Type inference in function `infer` changed significantly.
We no longer instantiate the polymorphic types of variables and generalize types at let bindings, but instantiate at function calls and generalize at function calls and function definitions instead.
To infer the types of functions, we first extend the environment with the types of the parameters, which might be annotated.
We remember all new type variables that appear in parameter types, so that we can later make sure that none of them was unified with a polymorphic type.
We then infer the type of the function body using the extended environment, and instantiate it unless it's annotated.
Finally, we generalize the resulting function type.

大幅に変更された`infer`関数で型推論します。
私たちはもはや、変数の多相型をインスタンス化とletバインディングで型を一般化しませんが、その代わりに関数呼び出しでインスタンス化し、関数呼び出しと関数定義で一般化します。
関数の型を推論するために、我々は最初に注釈を付けることが可能性があるパラメータの型と環境を拡張します。
我々は、後でそれらのどれもが多様型で単一化れていないことを確認することができるように、パラメータ型に表示されるすべての新しい型変数を覚えています。
それから、拡張された環境を使用して、関数本体の型を推論し、それは注釈付きだ場合を除き、それをインスタンス化します。
最後に、我々は結果の関数の型を一般化します。

To infer the type of function application we first infer the type of the function being called, instantiate it and separate parameter types from function return type.
The core of the algorithm is infering argument types in the function `infer_args`.
After infering the type of the argument, we use the function `subsume` (or `unify` if the argument is annotated) to determine if the parameter type is an instance of the type of the argument.
When calling functions with multiple arguments, we must first subsume the types of arguments for those parameters that are *not* type variables, otherwise we would fail to typecheck applications such as `rev_apply(id, poly)`, where `rev_apply : forall[a b] (a, a -> b) -> b`, `poly : (forall[a] a -> a) -> pair[int, bool]` and `id : forall[a] a -> a`.
Infering type annotation `expr : type` is equivalent to inferring the type of a function call `(fun (x : type) -> x)(expr)`, but optimized in this implementation of `infer`.

関数適用の型を推論するためには、まず、関数の型が呼び出され推論して、関数の戻り値の型とは別のパラメータ型をインスタンス化します。
アルゴリズムのコアは、関数 `infer_args` における推論引数型です。
引数の型を推論した後、我々は、パラメータの型が引数の型のインスタンスであるかどうかを判断するためには、関数 `subsume`（または引数が注釈されている場合は`unify`）を使用します。

Extensions
----------

拡張
--------

|English|日本語|
| --- | --- |
|describe|説明する|
|desirable|望ましい|

Daan Leijen also published a reference implementation ([.zip][hmf-ref]) of HMF, written in Haskell.
In addition to the type inference algorithm describe in his paper, he implemented an interesting extension to the algorithm that significantly improves the usability of HMF.
In **Overview** we saw that in order to create a list of polymorphic functions `ids : list[forall[a] a -> a]`, the programmer must add a type annotation `let ids = single(id : forall[a] a -> a)`, otherwise HMF infers the least polymorphic type.
The type annotation must be provided on the *argument* to the function `single`, in order to prevent the argument type from being instantiated.
However, it would be more desirable for the programmer to be able to specify the type of the *result* of function `single`:

Daan Leijen はまた Haskell で書かれた HMF のリファレンス実装を ([.zip][hmf-ref]) 公開されました。
彼の論文の推論アルゴリズムを記述する型に加えて、彼はかなりHMFの使い勝手を向上させるアルゴリズムに興味深い拡張機能を実装しました。
**概要**では、順番に多相関数の `ids : list[forall[a] a -> a]` のリストを作成することを見ましたが、プログラマは `let ids = single(id : forall[a] a -> a)` 型注釈を追加する必要があり、そうでない場合HMFは少なくとも多相型を推論します。
インスタンス化されるの引数の型を防止するためには、型注釈は、関数 `single`の*引数*に与えられなければなりません。
しかし、プログラマが関数 `single`の*結果*の型を指定できるようにすることがより望ましいでしょう。

```
let ids = single(id) : list[forall[a] a -> a]
```

|English|日本語|
| --- | --- |
|propagating|伝搬|
|propagate|伝搬する|
|indicate|示す、物語る|
|whether|かどうか|

We can implement this in the type inference algorithm by *propagating* the information about *expected types* from function result type to function arguments and to parameter types of functions expressions.
To function `infer` we add two additional arguments `maybe_expected_ty`, for optionally specifying the resulting type, and `generalized`, which indicates whether the resulting type should be generalized or instantiated.
To infer the type of function expression, we annotate the unannotated parameters with expected parameter types and use the expected return type to infer the type of the function body.
To propagate the expected type through a function application, we first unify it with the function return type.
Then we infer the types of the arguments, taking care to first infer the annotated arguments and to infer arguments for parameters which are type variables last.

私たちは、関数の引数と関数式のタイプのパラメータには、関数結果の型から*予想される型*に関する情報を*伝播*させることによって型推論アルゴリズムでこれを実装することができます。
関数inferのために、`maybe_expected_ty`が必要に応じて結果の型を指定し、`generalized`が結果の型は一般またはインスタンス化する必要があるかどうかを示す、我々は2つの引数を追加します。
関数式の型を推論するために、我々は、予想されるパラメータ型を持つ注釈のないパラメータに注釈を付けると、関数本体の型を推論することが期待戻り値の型を使用します。
関数アプリケーションから予期される型を伝播するために、我々は最初の関数の戻り値の型とそれを単一化。
その後、我々は最初の注釈付き引数を推測するために、最後の型変数であるパラメータの引数を推測するために世話をして、引数の型を推論。

|English|日本語|
| --- | --- |
|invariant|不変|

We cannot propagate the expected type through a function application if the return type of the function is a type variable.
For example, for function `head : forall[a] list[a] -> a`, propagating the result type in `head(ids) : int -> int` would instantiate the parameter type to `list[int -> int]`, which is not an instance of `list[forall[a] a -> a]` (since this type system is invariant).

関数の戻り値の型が型変数であれば我々は、関数アプリケーションから予想型を伝播することはできません。
例えば、関数`head : forall[a] list[a] -> a`では、`head(ids) : int -> int`での結果の型を伝播することは`list[forall[a] a -> a]`(この型システムは不変であるため) のインスタンスではない`list[int -> int]`へのパラメータの型を、インスタンス化することになります。

|English|日本語|
| --- | --- |
|unambiguous|明白な|

This extension also allows programmers to write anonymous functions with polymorphic arguments without annotations in cases when the function type is unambiguous:

この拡張は、関数の型が明確なときにプログラマは場合によっては注釈なしに多相型の引数を持つ無名関数を書くことができます：

```
let special = fun (f : (forall[a] a -> a) -> (forall[a] a -> a)) -> f(id) : forall[a] a -> a in
special : ((forall[a] a -> a) -> (forall[a] a -> a)) -> forall[a] a -> a

special(fun f -> f(f))
```

[system_f]: http://en.wikipedia.org/wiki/System_F
[hmf]: http://research.microsoft.com/apps/pubs/default.aspx?id=132621
[mlf]: http://gallium.inria.fr/~remy/work/mlf/
[hmf-ref]: http://research.microsoft.com/en-us/um/people/daan/download/hmf-prototype-ref.zip
