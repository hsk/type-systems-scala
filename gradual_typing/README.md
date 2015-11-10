Gradual Typing
==============


<sup><sub>
rather かなり
undeiable un-deny-able 否定しがたい 申し分ない
denote 示す
</sub></sup>

> <sup><sub>
This is a rather small extension of the Damas-Hindley-Milner unification-based type inference algorithm, which allows programmers to combine static and dynamic types in a single language.
In addition to standard types and type schemes (polymorphic types), it supports a special *dynamic* type, which is automatically cast to any other type as necessary.

これは、プログラマが、単一の言語で静的型か動的型の組み合わせを許す、ダマ・ヒンドリー - ミルナ単一化ベース型推論アルゴリズムのかなり小さな拡張です。
標準型と型スキーム(多相型)に加えて、必要に応じて自動的に他の型にキャストされた特別な*動的*型をサポートします。


> <sup><sub>
Overview

概要
--------

<sup><sub>
surge うねり
successor 後継
provide 提供
earlier 以前
</sub></sup>

> <sup><sub>
Statically and dynamically typed languages both have their undeniable strenghts and weaknesses, and are both used to construct huge, complex software systems.
In the recent years, there has been a surge of interest in combining their benefits: type `dynamic` was added to C#, an `invokedynamic` instruction was added to JVM, JavaScript successors TypeScript and Dart provide optional type annotations, ...
The general idea is to give the programmers the benefits of static typing (earlier detection of errors and faster execution speed) while allowing them to bypass the static type system when necessary or convenient (e.g. when protoyping new functionality or handling dynamic data formats such as JSON). 

静的および動的型付け言語の両方が、その紛れもない長所と短所があり、両方の巨大な、複雑なソフトウェアシステム構築するために使用されます。
近年では、その利点を組み合わせることへの関心の高まりがありました： `dynamic` 型がC#に追加され、`invokedynamic` 命令がJVMに追加され、JavaScriptの後継であるTypeScriptとDartは、オプションの型注釈を提供する、など...
一般的な考え方は、（例えば、新しい機能のプロトタイプを作るときや、JSONなどの動的データ・フォーマットを処理する）ときに必要または便利な、静的型システムをバイパスするためにそれらを可能にし、プログラマに静的型付け（エラーや実行速度の早期検出）の恩恵を与えます。


<sup><sub>
collaborators 共同研究者
notation 表記
denote 意味する
explanation 説明
</sub></sup>

> <sup><sub>
This implementation explores the union of static and dynamic typing from a type-theoretic perspective, following the work of Jeremy G. Siek and collaborators.
First, the topics of *gradual type-checking* and *gradual type-inference* are discussed, followed by an explanation of a gradual type-inference algorithm and a discussion of related research.
The notation `?` will be used to denote the dynamic type.

この実装は、Jeremy G. Siek氏と共同研究者の仕事に続いて、型理論的な視点から静的および動的型付けのユニオンについて検討します。
まず、*gradual type-checking*と*gradual type-inference*の話題を考察し、gradual 型推論アルゴリズムの説明と関連研究について議論します。
表記 `?` は動的型を示すために使用されます。


> <sup><sub>
Gradual type-checking

Gradual 型検査
---------------------

<sup><sub>
stated goal 述べた目標
fully-annotated 完全に注釈された
completely 完全に
Fulfilling 充実しました
presence プレゼンス、存在
turned out 判明
quite かなり
elusive 理解しにくい
seamlessly 継ぎ目なく
e.g. 例えば
allowed 許可されました
as well as 及び
achieve 達成します
attempted 試み
transitive 推移的
relation 関係
something 何か
</sub></sup>

> <sup><sub>
The stated goal of gradual type-checking is very simple:
programs which are fully-annotated (every term has a static type) are completely (statically) type-safe.
Fulfilling this goal in presence of dynamic types has turned out to be quite elusive.
To achieve the feeling of dynamically-typed languages, we want to be able to seamlessly transition between statically and dynamically typed values.
This means that implicit casts from type `?` to static types (e.g. `int`, `bool` and `int -> int`) must be allowed, as well as implicit casts from static types back to type `?`.
One way to achieve this is to use subtyping; this was attempted by Satish Thatte in his paper Quasi-static typing [1].
However, subtyping is a transitive relation, meaning that if `int <: ?` and `? <: bool`, then `int <: bool`, which is not something we want.

漸進的型チェックの目標は非常にシンプルです:
完全に注釈付けられた(すべての項が、静的な型を持つ)プログラムは完全に(静的に)型安全です。
動的型の存在で、この目的を果たすことは非常にとらえどころのないことが分かりました。
動的型付け言語の感覚を達成するために、我々は静的な型の値と動的型の値の間でシームレスに変換できるようにします。
この意味は`?`型から静的型(例えば`int`、`bool`そして`int -> int`)への暗黙的型変換、及び静的型から`?`型へ戻す暗黙的型変換が許される必要があります。

<sup><sub>
their 彼らの
propose 提案します
treating 処理
In short 要するに
consistent 一貫性のある
themselves それら自身
</sub></sup>

> <sup><sub>
In their paper [Gradual Typing for Functional Languages][gradual], Jeremy G. Siek and Walid Taha propose a different way of treating `?` based on *type consistency* (`~`), which is *not* a transitive relation.
In short, `?` is consistent with everything, base types are consistent only with themselves, and function types are consistent if their parameter and return types are consistent.

彼らの論文では、[関数型言語のための段階的タイピング][gradual]、Jeremy G. Siek と Walid Tahaは、`?` に基づく 推移的関係では*ない* *型の一貫性*(`~`) を維持する別の方法を提案しました。
要するに、`?`は すべてと一致し、ベースの型は自分自身だけと一致して、関数の型はパラメータと戻り値の型が一致している場合に一致します。

```
? ~ int
int -> bool ~ ?
int -> int ~ ? -> int
? -> int ~ bool -> int
? -> int ~ (int -> int) -> int
```

<sup><sub>
Note 注意
transitive 他動詞
even though たとえ
Furthermore さらに
consistency 一貫性
symmetric 対称の
considerably かなり
simpler 単純な
compared 比べ
</sub></sup>

> <sup><sub>
Note that since `~` is not transitive, we do not have `int -> int ~ bool -> int` even though `int -> int ~ ? -> int` and `? -> int ~ bool -> int`.
Furthermore, type consistency is symmetric, which makes the type-checking algorithm considerably simpler (compared to subtyping).

`~` は推移的ではないので注意が必要で、`int -> int ~ ? -> int`かつ`? -> int ~ bool -> int`だとしても、`int -> int ~ bool -> int`ではありません。
また、型の一貫性は対称なため、（サブタイプと比較して）型チェックアルゴリズムはかなり簡単です。

<sup><sub>
compatibility 互換性
treated 処理された
except 除く
occurrence 出現
</sub></sup>

> <sup><sub>
Using type compatibility, we can type-check a gradually-typed program by implicitly converting a type into any consistent type as necessary.
This is quite similar to the way unbound type variables are treated, except that each occurrence of type `?` is treated as a fresh type variable.

型の互換性を使用して、我々は、暗黙のうちに、必要な一貫性のある型に型変換することによって前進的型付けされたプログラムを型検査することができます。
これは、結合していない型変数を処理する方法と非常によく似ており、それ以外の`?`型の各出現は、新鮮な型の変数として扱われます。


> <sup><sub>
Gradual type inference

Gradual 型推論
----------------------

<sup><sub>
practical 実用的
should すべきです
usually 通常
omit 省略する
Meanwhile 一方
obstruct 妨害します
</sub></sup>

> <sup><sub>
To make this type system practical, we must extend the gradual type-checking algorithm with a gradual type inference algorithm.
The type inference algorithm should do what type inference algorithms usually do: allow the programmer to omit most, if not all, type annotations in programs that do not use dynamic types (i.e. that have no variables and parameters with type `?` and call no functions returning `?`).
Meanwhile, the algorithm should not obstruct the programmer when he or she wants to use dynamic types.

この型システムを実用的にするために、我々は漸進的型推論アルゴリズムで漸進的型チェックアルゴリズムを拡張する必要があります。
型推論アルゴリズムは、通常プログラマがほとんど省略することができ、すべてではないが、動的なタイプの使用しないプログラムに型注釈をかける(つまり、型`?`とは変数とパラメータを持たず、`?`を返す関数を呼び出さない)型推論アルゴリズムである必要があります。
一方、彼または彼女が動的な型を使用したい場合、アルゴリズムは、プログラマを邪魔してはいけません。

```
let f1(x) = x + 1

let f2(x) = if not x then x + 1 else 0

let f3(x : ?) = if not x then x + 1 else 0
```

<sup><sub>
correctly 正しく
rejected 拒否
particular 特定の
In particular 特に
desired 希望
outcome 結果
should すべきです
deferring 延期
</sub></sup>

> <sup><sub>
The type inference algorithm should correctly infer that the type of parameter `x` of function `f1` is `int`, based on its use as an argument to the `+` operator.
Function `f2` should be rejected by the algorithm, because no statically-typed value can be used both as a `bool` and an `int`.
In particular, inferring `x : ?` for function `f2` is not a desired outcome of the type inference algorithm.
However, it should infer type `? -> int` for function `f3`, deferring the type-checks to runtime (at runtime, `f3(true)` will not result in a type error).

型推論アルゴリズムは正しく、`+`演算子の引数としての使用に基づいて、関数`f1`のパラメータ`x`の型が`int`であることを推論する必要があります。
静的に型付けされた値が `bool` と `int` の両方として使用することはできないので、関数 `f2` は、アルゴリズムによって拒否されるべきです。
具体的には、関数`f2`の推論`x : ?`は型推論アルゴリズムの望ましい結果ではありません。
しかし、ランタイムに型チェックを遅延して(実行時に、f3(true)は型エラーにはなりません)、`f3`関数の型`? -> int`を推論する必要があります。

<sup><sub>
propagate 伝播します
introduce 紹介します
formalized 正式な
informative 有益な
requirement 要件
constraining 制約
</sub></sup>

> <sup><sub>
In general, we want the inference algorithm to propagate `?`, but we do not want it to introduce any fresh `?`.
This idea was formalized by Jeremy G. Siek and Manish Vachharajani in their paper [Gradual Typing with Unification-based Inference][inference] using the *less or equally informative* relation and the requirement that the types assigned to unifiable type variables are at least as informative as any of the types constraining the type variable.

一般的に、我々の推論アルゴリズムは、`?`を伝播したいのですが、我々はそれが任意の新鮮な`?`を導入したくはありません。
このアイデアは、Jeremy G. SiekとManish Vachharajaniの論文[Gradual Typingの単一化ベース推論][inference]によって定式化され、単一化可能型変数に割り当てられたタイプは、少なくとも型変数を制約するタイプのいずれかのように情報を提供している*小さいか等しい有益な*関係と要件を使用。　(TODO)

<sup><sub>
requirement 要件
satisfied 満足
ordinary 普通
becomes なります
substituted 置換されました
essence エッセンス
</sub></sup>

> <sup><sub>
In the type inference algorithm, this requirement is satisfied by a very simple rule: an ordinary (non-dynamic) type variable unified with a dynamic type becomes a dynamic type variable, and an ordinary or dynamic type variable unified with a type other than `?` is substituted for that type.
In essence, the type inference algorithm tracks the lower bound of a type variable, which can only move from less informative to more informative.

型推論アルゴリズムでは、この要件は、非常に単純なルールによって満たされます：動的型は、動的型変数になると一体化し、変数の型、通常の（非動的）、および以外の型で単一化普通または動的型の変数は`?`その型に代入されます。
本質的には、型推論アルゴリズムは、下型変数のバインドを追跡し、唯一のより多くの情報を小さい有益から移動することができます。

> <sup><sub>
Implementation

実装
--------------

<sup><sub>
describe 説明します
consisting なります
constraint 制約
inspired インスピレーションを受けた
similar 同様の
interleaving インターリービング
significant 重要な
freezing 凍結
reused 再利用
multiple times 複数回
</sub></sup>

> <sup><sub>
In their paper, Siek and Vachharajani describe a 2-step type inference algorithm, consisting of a syntax-directed constraint generation step, followed by a constraint solving step.
This implementation, while inspired by their algorithm, is instead more similar to Algorithm W, interleaving constraint generation and constraint solving steps.
It extends their algorithm in two significant ways: it handles let-polymorphism (in the same manner as Algorithm W) and supports *freezing* of dynamic types in variables bound by `let` expressions, allowing them to be reused multiple times in different contexts.

彼らの論文では、Siek氏とVachharajaniは、制約の解決ステップに続く、構文指向制約生成工程からなる、2段型推論アルゴリズムを記述します。
この実装は、そのアルゴリズムに触発されながら、代わりにアルゴリズムWに似ています、インターリーブ制約の生成と制約の解決手順を実行します。
これは、2つの重要な方法で自分のアルゴリズムを拡張します:それは（アルゴリズムWと同じように）させて、多型を処理し、`let`式によって束縛された変数の動的型の*冷凍*をサポートし、許可それらを異なるコンテキストで複数回再使用することができます。

<sup><sub>
standalone スタンドアロン
whether かどうか
having ました
yet-unknown まだ不明
</sub></sup>

> <sup><sub>
The main changes between `algorithm_w` and this implementation can be seen in file `infer.ml` [here][git-diff].
We extend the expressions of `algorithm_w` by adding type annotations to function parameters (`fun (x : int) -> x + 1`), let-bound variables (`let x : ? = 1`) and standalone expressions (`f(x) : int`).
The setting `dynamic_parameters` controls whether function parameters without type annotations are treated as having dynamic types (as in dynamically-typed languages) or as statically-typed variables with yet-unknown types.
For examples, `fun g -> g(true)` can be treated as `fun (g : ?) -> g(true)` or as `fun (g : some[a] a) -> g(true)` (syntax sugar allows the latter to be written as `fun (g : _) -> g(true)`), for which the system infers the type `forall[a] (bool -> a) -> a`.

`algorithm_w`とこの実装間の主な変更点は、ファイル`infer.ml`[こちら][git-diff]で見られます。
我々は、関数パラメータ(`fun (x : int) -> x + 1`)、letバインドされた変数 (`let x : ? = 1`) そしてスタンドアロンの式 (`f(x) : int`)へ型注釈を追加することにより、`algorithm_w`の式を拡張しました。

`dynamic_parameters`の設定は、型注釈のない関数のパラメータが、まだ未知の型で（動的型付け言語のように）動的な型として、または静的に型付けされた変数持つものとして扱われているかどうかを制御します。

<sup><sub>
introduce 紹介します
represent 表します
as soon as できるだけ早く
instantiated インスタンス化
distinguished 区別
unlike 異なり、
indeed 確かに
Conversely 逆に
ordinary 普通
so そう
frozen フローズン
</sub></sup>

> <sup><sub>
We also introduce type constructor `TDynamic`, used to represent `?`.
However, `TDynamic` is only used to represent type `?` for variables in type environment; as soon as the variable is used, `TDynamic` is instantiated and replaced with a fresh *dynamic type variable*, which is represented by constructor `Unbound` but distinguished from an *ordinary type variable* with a boolean flag.
This is not unlike the treatment of polymorphic types in Algorithm W; indeed, when a variable with a polymorphic type is used, its type is instantiated by replacing all occurrences of generic type variables with fresh ordinary type variables. Conversely, just as polymorphic types can be recovered at let-bindings by generalizing free ordinary type variables, so can dynamic type variables be *frozen* at let-bindings by replacing them with `TDynamic` types (this is controlled by the setting `freeze_dynamic`).

我々は、また、`?`を表すために使用される、型コンストラクタ`TDynamic`を導入します。
しかしながら、`TDynamic`は、型が環境における変数の型TTTを表すために使用されます; すぐに変数を使用するように、`TDynamic`がインスタンス化され、新鮮な*動的型変数*に置き換えられ、ブールフラグと*通常の型変数*から`Unbound`が、著名なコンストラクタによって表されます。
これは、アルゴリズムW中の多型の型の処置と似ていなくもではありません; ポリモーフィック型の変数が使用されている場合、実際に、その型は、新鮮な通常の型変数でジェネリック型変数のすべての出現を置換することにより、インスタンス化されます。
逆に、ポリモーフィック型は自由通常タイプの変数を一般化してみましょうバインディングで回収することができるので、動的型の変数は、（これは設定`freeze_dynamicによって制御されている` TDynamic`タイプに置き換えることにより、レットバインディングで*凍結*することができます同じように`）。

<sup><sub>
duplicate 複製
</sub></sup>

> <sup><sub>
The idea that makes polymorphic types polymorphic and dynamic types dynamic is that fresh type variables can be unified with any other type.
However, each type variable can only be unified once, with a single type.
This can be a problem when using functions such as `duplicate : forall[a] a -> pair[a, a]`, which duplicate type variables. The following results in an error:

アイデアは多相型の多相と動的型の動的は、新鮮な型変数が他のタイプに単一化することができるということです。
しかし、それぞれの型変数は、単一の型で、一度単一化することができます。
型変数を複製するような`duplicate : forall[a] a -> pair[a, a]`などの関数を使用する場合に問題となり得ます。
エラーで以下の結果が得られます：

```
choose(pair([1], [true]), duplicate([]))

# ERROR: cannot unify types bool and int
```

<sup><sub>
avoid 避けます
issue 問題
equivalent 同等の
</sub></sup>

> <sup><sub>
To avoid this issue with dynamic types, we duplicate dynamic type variables after every function call, which is equivalent to first generalizing and then instantiating the result type, the trick used in `first_class_polymorphism`.

動的型でこの問題を回避するには、我々は、すべての関数呼び出し後の動的型の変数を複製し、これは最初の一般化に相当し、その結果タイプをインスタンス化、`first_class_polymorphism`で使用するトリック。

```
let x : ? = 1

let y = choose(pair(1, true), duplicate(x))

# y : pair[int, bool]
```


> <sup><sub>
Discussion

考察
----------

<sup><sub>
Similar 類似
explored 探検
looking 見ること
through 経て
citing 引用
above 上記
</sub></sup>

> <sup><sub>
This implementation focuses on gradual typing in the context of functional languages.
Similar ideas, but in a context of object-oriented languages, was explored by Jeremy Siek and Walid Taha in the paper [Gradual Typing for Objects][objects].
It was also researched by other authors, for example in [2], [3] and [4].
More recent research can be found by looking through the papers citing the above on Google Scholar. 

この実装では、関数型言語の文脈で gradual typing に焦点を当てています。
同様のアイデアは、ただしオブジェクト指向言語の文脈において、Jeremy SiekとWalid Tahaの論文[オブジェクトの漸進的型付け][objects]によって調査されました。
また、他の著者らによって研究された例があります[2]、[3]、[4]。
より最近の研究は、Google Scholarの上の、上記引用論文を参照ください。

<sup><sub>
Although であるが
complicated 複雑な
tricky トリッキー
especially 特に
accurate 正確な
applied 適用されました
correctly 正しく
blame 非難
culminated 結実
theorem 定理
states 状態
portion 部分
</sub></sup>

> <sup><sub>
Although gradual type inference is not complicated, the implementation of a gradually typed language can be tricky, especially when a statically-typed function is used in dynamic code or when a dynamically-typed function is cast to a static type.
One issue is accurate reporting of runtime errors; for example, when a function `inc : int -> int` is cast to a dynamic type and called with the argument `true`, the system should report an error that the function was *called* with an argument of the wrong type.
However, if a dynamic function `fun (x : ?) -> not x` is cast to type `bool -> int` and applied to the argument `true`, the error should say that the function *returned* a value of the wrong type.
The research into the topic of correctly assigning *blame* in gradually-typed programs has culminated in the *blame theorem*, which states that "well-typed programs cannot be blamed", meaning that blame is always assigned to the dynamically-typed portion of the program.
A nice overview of this topic is provided in [5].

漸進的型推論は複雑ではありませんが、前進的型付けされた言語の実装上で、動的型付けされた関数が静的な型にキャストされたとき、あるいは静的型付けされた関数が動的コードで使用されたときに、特に注意が必要です。
1つの問題は、実行時エラーの正確な報告です; 例えば、関数 `inc : int -> int` が動的型にキャストして、引数`true`で呼ばれたときに、システムは関数が間違った型の引数を指定して*呼ばれた*というエラーを報告する必要があります。
しかし、動的関数`fun (x : ?) -> not x`が`bool -> int`型にキャストされ、引数`true`を適用された場合は、関数が誤ったの値の型*返された*ことを報告する必要があります。
前進的型付けされたプログラムで正しく割り当て*非難*のトピックの研究は、責任は常に動的に型付けされた部分に割り当てられていることを意味し、「よく型付けされたプログラムは非難することはできない」と述べている*の非難の定理*、で最高潮に達していますプログラムの。
このトピックの素晴しい概要は[5]で設けられています。

<sup><sub>
related 関連しました
issue 問題
efficiently 効果的に
translating 翻訳
moment 瞬間
straightforward 簡単な
separately 別々
However しかし、
recursively 再帰的に
just ちょうど
keep 保ちます
upon 時
space-wise 空間的
time-wise 時間的
inefficient 非効率的な
immediately すぐに
elaborated 精巧な
deeply 深く
explained 説明
</sub></sup>

> <sup><sub>
A related issue is that of efficiently translating type casts and reporting errors at the right moment.
The straightforward way of implementing function casts is to cast the argument and the return value separately: if `f` has type `? -> ?`, then the cast `f : int -> bool` can be compiled as `fun (x : int) -> (f(x) : bool)`.
However, if we have two functions that call each other recursively (e.g. the [basic example][mutual-recursion] of `is_odd` and `is_even`), one static and the other dynamic, a naive implementation would just keep adding cast upon cast, which would result in space-wise and time-wise inefficient execution.
A related issue is how to implement casts such as `((inc : int -> int) : ?) : bool -> bool` - should the type error be reported immediately, or only when the function is first used?
The first issue is touched upon in [5], while the second is elaborated more deeply in [6] and also explained in a [series of blog posts][blog-post].

関連の問題は、適切なタイミングで効率的に型キャストを翻訳し、報告するエラーです。
関数はキャスト実装の簡単な方法は、別々の引数と戻り値をキャストすることです：`f`が`? -> ?`型を持つ場合、その後、キャスト`f : int -> bool`は`fun (x : int) -> (f(x) : bool)`としてコンパイルすることができます。

<sup><sub>
Finally 最後に
explains 説明
parametrically パラメトリック
translating 翻訳
inspected 検査
any way どのような方法
e.g. 例えば
typecase 活字ケース
unwrapped 開封されました
corresponding 対応します
This way この方法
sure 確信して
really 本当に
recover 回復します
proof 証拠
identity アイデンティティ
either どちらか
diverge 発散します
remains 遺跡
i.e. すなわち
safely 安全に
</sub></sup>

> <sup><sub>
Finally, [7] explains how to implement safe casts from dynamic functions to parametrically polymorphic types (such as `forall[a] a -> a`) by using *dynamic sealing*.
For example, if `f` has type `? -> ?`, we can implement the cast `f : forall[a] a -> a` by translating it into `fun x -> unwrap@1 (f (wrap@1 x))`, where the wrapped value `(wrap@1 x)` cannot be inspected in any way (e.g. using typecase) and can only be unwrapped by the corresponding `unwrap@1`.
This way, we can be sure that the argument `x` is really used parametrically and we can recover the proof that the only values of type `forall[a] a -> a` are the identity function and functions that either diverge or raise an error.
However, it remains unclear if this idea can be extended to polymorphic container types, i.e. how to safely implement casts such as `f : forall[a] a -> list[a]`.


> <sup><sub>
References

参考文献
----------

[1] S Thatte. *Quasi-static typing.* 1989

[2] T Lindahl, K Sagonas. *[Practical Type Inference Based on Success Typings](http://www.it.uu.se/research/group/hipe/papers/succ_types.pdf).* 2006

[3] A Rastogi, A Chaudhuri, B Hosmer. *[The Ins and Outs of Gradual Type Inference](http://www.cs.umd.edu/~avik/papers/iogti.pdf)*. 2012

[4] T Wrigstad, F Z Nardelli, S Lebresne, J Ostlund, J Vitek. *[Integrating Typed and Untyped Code in a Scripting Language](https://www.cs.purdue.edu/homes/jv/pubs/popl10.pdf)*. 2009

[5] J G Siek, P Thiemann, P Wadler. *[Blame, coercions, and threesomes, precisely](http://homepages.inf.ed.ac.uk/wadler/papers/coercions/coercions.pdf)*. 2009

[6] J G Siek, R Garcia. *[Interpretations of the Gradually-Typed Lambda Calculus](http://wphomes.soic.indiana.edu/jsiek/files/2013/06/igtlc.pdf)*. 2012

[7] A Ahmed, R B Findler, J G Siek, P Wadler. *[Blame for All](http://ecee.colorado.edu/~siek/blame-forall-2011.pdf)*. 2011



[gradual]: http://ecee.colorado.edu/~siek/pubs/pubs/2006/siek06:_gradual.pdf
[inference]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.84.8219&rep=rep1&type=pdf
[git-diff]: https://github.com/tomprimozic/type-systems/compare/7320bae...1ae7906#diff-4
[objects]: http://www.cs.colorado.edu/~siek/gradual-obj.pdf
[mutual-recursion]: http://en.wikipedia.org/wiki/Mutual_recursion#Basic_examples
[blog-post]: http://siek.blogspot.co.uk/2012/09/interpretations-of-gradually-typed.html
