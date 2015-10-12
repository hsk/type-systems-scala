Gradual Typing
==============

rather かなり
undeiable un-deny-able 否定しがたい 申し分ない
denote 示す


This is a rather small extension of the Damas-Hindley-Milner unification-based type inference algorithm, which allows programmers to combine static and dynamic types in a single language.
In addition to standard types and type schemes (polymorphic types), it supports a special *dynamic* type, which is automatically cast to any other type as necessary.

これは、プログラマは、単一の言語で静的および動的なタイプの組み合わせることができダマ・ヒンドリー - ミルナー統一ベース型推論アルゴリズムのかなり小さい拡張したものです。
標準タイプと型スキーム（多相型）に加えて、必要に応じて自動的に他の型にキャストされた特別な*動的*型をサポートしています。


Overview
--------

概要
--------

Statically and dynamically typed languages both have their undeniable strenghts and weaknesses, and are both used to construct huge, complex software systems.
In the recent years, there has been a surge of interest in combining their benefits: type `dynamic` was added to C#, an `invokedynamic` instruction was added to JVM, JavaScript successors TypeScript and Dart provide optional type annotations, ...
The general idea is to give the programmers the benefits of static typing (earlier detection of errors and faster execution speed) while allowing them to bypass the static type system when necessary or convenient (e.g. when protoyping new functionality or handling dynamic data formats such as JSON). 

静的および動的型付け言語の両方が、その紛れもない長所と短所があり、両方の巨大な、複雑なソフトウェアシステム構築するために使用されます。
近年では、その利点を組み合わせることへの関心の高まりがあった：型 `dynamic` は `invokedynamic` 命令がJVMに追加された、C#のに加え、JavaScriptが活字とダートは、オプションの型注釈を提供後継者、...
一般的な考え方は、静的型システムをバイパスするためにそれらを可能にしながら、プログラマに静的型付け（エラーや実行速度の早期検出）の恩恵を与えることであるときに（必要または便利な例えば、新しい機能をprotoypingや、JSONなどの動的データ・フォーマットを処理するとき）。

This implementation explores the union of static and dynamic typing from a type-theoretic perspective, following the work of Jeremy G. Siek and collaborators.
First, the topics of *gradual type-checking* and *gradual type-inference* are discussed, followed by an explanation of a gradual type-inference algorithm and a discussion of related research.
The notation `?` will be used to denote the dynamic type.

この実装は、ジェレミーG. Siek氏と共同研究者の仕事に続いて、型理論的な視点から静的および動的型付けのユニオンについて検討します。
まず、*gradual type-checking*と*gradual type-inference*のトピックは gradual 型推論アルゴリズムの説明と関連研究についての議論が続く、議論されています。
表記 '?' はダイナミック型を示すために使用されます。


Gradual type-checking
---------------------

Gradual 型検査
---------------------


The stated goal of gradual type-checking is very simple:
programs which are fully-annotated (every term has a static type) are completely (statically) type-safe.
Fulfilling this goal in presence of dynamic types has turned out to be quite elusive.
To achieve the feeling of dynamically-typed languages, we want to be able to seamlessly transition between statically and dynamically typed values.
This means that implicit casts from type `?` to static types (e.g. `int`, `bool` and `int -> int`) must be allowed, as well as implicit casts from static types back to type `?`.
One way to achieve this is to use subtyping; this was attempted by Satish Thatte in his paper Quasi-static typing [1]. However, subtyping is a transitive relation, meaning that if `int <: ?` and `? <: bool`, then `int <: bool`, which is
not something we want.

緩やかな型チェックの述べた目標は非常に簡単です：
完全に注釈を付けている(すべての項は、静的な型を持つ)プログラムは完全に(静的に)タイプセーフです。
ダイナミック型の存在で、この目的を果たすことは非常にとらえどころのないことが判明しました。
動的型付け言語の感覚を達成するために、我々はシームレスに静的におよび動的型付けの値との間で移行できるようにします。

In their paper [Gradual Typing for Functional Languages][gradual], Jeremy G. Siek and Walid Taha propose a different way of treating `?` based on *type consistency* (`~`), which is *not* a transitive relation.
In short, `?` is consistent with everything, base types are consistent only with themselves, and function types are consistent if their parameter and return types are consistent.

彼らの論文では、[関数型言語のための段階的タイピング][gradual]、ジェレミーG. Siek氏とWalid Tahaは、(`~`)`？`*に基づく型の一貫性*を治療する別の方法を提案し、これは推移的関係では*ありません*。
要するに、 `?` すべてと一致して、ベースのタイプは自分自身だけで一致しており、それらのパラメータと戻り値の型が一致している場合、関数の型は一致しています。

```
? ~ int
int -> bool ~ ?
int -> int ~ ? -> int
? -> int ~ bool -> int
? -> int ~ (int -> int) -> int
```

Note that since `~` is not transitive, we do not have `int -> int ~ bool -> int` even though `int -> int ~ ? -> int` and `? -> int ~ bool -> int`.
Furthermore, type consistency is symmetric, which makes the type-checking algorithm considerably simpler (compared to subtyping).

`~` は推移的ではないので`int -> int ~ ? -> int`かつ`? -> int ~ bool -> int`であるにもかかわらず、我々は、`int -> int ~ bool -> int`を持っていないことに注意してください。
また、型の一貫性は対称なため、（サブタイプと比較して）型チェックアルゴリズムはかなり簡単になります。

Using type compatibility, we can type-check a gradually-typed program by implicitly converting a type into any consistent type as necessary.
This is quite similar to the way unbound type variables are treated, except that each occurrence of type `?` is treated as a fresh type variable.

型の互換性を使用して、我々は、暗黙のうちに、必要な一貫性のある型に型変換することによって徐々に型付けされたプログラムを型検査することができます。
これは、結合していない型変数を処理する方法と非常によく似ており、その以外のタイプ`?`の各出現は、新鮮な型の変数として扱われます。


Gradual type inference
----------------------

Gradual 型推論
----------------------

To make this type system practical, we must extend the gradual type-checking algorithm with a gradual type inference algorithm.
The type inference algorithm should do what type inference algorithms usually do: allow the programmer to omit most, if not all, type annotations in programs that do not use dynamic types (i.e. that have no variables and parameters with type `?` and call no functions returning `?`).
Meanwhile, the algorithm should not obstruct the programmer when he or she wants to use dynamic types.

このタイプのシステムを実用的にするために、我々は緩やかな型推論アルゴリズムで緩やかな型チェックアルゴリズムを拡張する必要があります。
型推論アルゴリズムは、通常プログラマがほとんど省略することができ、すべてではないが、動的なタイプの使用しないプログラムに型注釈をかける(つまり、型`?`とは変数とパラメータを持たず、`?`を返す関数を呼び出さない)型推論アルゴリズムである必要があります。
一方、彼または彼女は動的な型を使用したい場合、アルゴリズムは、プログラマを邪魔してはいけません。

```
let f1(x) = x + 1

let f2(x) = if not x then x + 1 else 0

let f3(x : ?) = if not x then x + 1 else 0
```

The type inference algorithm should correctly infer that the type of parameter `x` of function `f1` is `int`, based on its use as an argument to the `+` operator.
Function `f2` should be rejected by the algorithm, because no statically-typed value can be used both as a `bool` and an `int`.
In particular, inferring `x : ?` for function `f2` is not a desired outcome of the type inference algorithm.
However, it should infer type `? -> int` for function `f3`, deferring the type-checks to runtime (at runtime, `f3(true)` will not result in a type error).

型推論アルゴリズムが正しく、パラメータの型`x`が関数`f1`は`int`であることを推論する必要があり、`+`演算子の引数としての使用に基づきます。
静的に型付けされた値が `bool` と `int` の両方として使用することはできないので、関数 `f2`は、アルゴリズムによって拒否されるべきです。
具体的には、関数`f2`の推論`x : ?`は型推論アルゴリズムの望ましい結果ではありません。
しかし、ランタイムに型チェックを遅延して(実行時に、f3(true)は型エラーにはなりません)、`f3`関数の型`? -> int`を推論する必要があります。


In general, we want the inference algorithm to propagate `?`, but we do not want it to introduce any fresh `?`.
This idea was formalized by Jeremy G. Siek and Manish Vachharajani in their paper [Gradual Typing with Unification-based Inference][inference] using the *less or equally informative* relation and the requirement that the types assigned to unifiable type variables are at least as informative as any of the types constraining the type variable.

一般的に、我々の推論アルゴリズムは、`?`を伝播したいのですが、我々はそれが任意の新鮮な`?`を導入したくはありません。
このアイデアは、Jeremy G. SiekとManish Vachharajaniの論文[Gradual Typingの単一化ベース推論][inference]によって定式化され、単一化可能型変数に割り当てられたタイプは、少なくとも型変数を制約するタイプのいずれかのように情報を提供している*小さいか等しい有益な*関係と要件を使用。


In the type inference algorithm, this requirement is satisfied by a very simple rule: an ordinary (non-dynamic) type variable unified with a dynamic type becomes a dynamic type variable, and an ordinary or dynamic type variable unified with a type other than `?` is substituted for that type.
In essence, the type inference algorithm tracks the lower bound of a type variable, which can only move from less informative to more informative.

型推論アルゴリズムでは、この要件は、非常に単純なルールによって満たされます：ダイナミック型は、ダイナミック型変数になると一体化し、変数の型、通常の（非動的）、および以外の型で単一化普通またはダイナミック型の変数は`?`その型に代入されます。
本質的には、型推論アルゴリズムは、下型変数のバインドを追跡し、唯一のより多くの情報を小さい有益から移動することができます。

Implementation
--------------

実装
--------------

In their paper, Siek and Vachharajani describe a 2-step type inference algorithm, consisting of a syntax-directed constraint generation step, followed by a constraint solving step.
This implementation, while inspired by their algorithm, is instead more similar to Algorithm W, interleaving constraint generation and constraint solving steps.
It extends their algorithm in two significant ways: it handles let-polymorphism (in the same manner as Algorithm W) and supports *freezing* of dynamic types in variables bound by `let` expressions, allowing them to be reused multiple times in different contexts.

彼らの論文では、Siek氏とVachharajaniは、制約の解決ステップに続く、構文指向制約生成工程からなる、2段型推論アルゴリズムを記述します。
この実装は、そのアルゴリズムに触発されながら、代わりにアルゴリズムWに似ています、インターリーブ制約の生成と制約の解決手順を実行します。
これは、2つの重要な方法で自分のアルゴリズムを拡張します:それは（アルゴリズムWと同じように）させて、多型を処理し、`let`式によって束縛された変数の動的型の*冷凍*をサポートし、許可それらを異なるコンテキストで複数回再使用することができます。

The main changes between `algorithm_w` and this implementation can be seen in file `infer.ml` [here][git-diff].
We extend the expressions of `algorithm_w` by adding type annotations to function parameters (`fun (x : int) -> x + 1`), let-bound variables (`let x : ? = 1`) and standalone expressions (`f(x) : int`).
The setting `dynamic_parameters` controls whether function parameters without type annotations are treated as having dynamic types (as in dynamically-typed languages) or as statically-typed variables with yet-unknown types.
For examples, `fun g -> g(true)` can be treated as `fun (g : ?) -> g(true)` or as `fun (g : some[a] a) -> g(true)` (syntax sugar allows the latter to be written as `fun (g : _) -> g(true)`), for which the system infers the type `forall[a] (bool -> a) -> a`.

`algorithm_w`とこの実装間の主な変更点は、ファイル`infer.ml`[こちら][git-diff]で見ることができます。
私たちは、パラメータを機能するように型注釈を追加することにより、`algorithm_w`の表現を拡張(`fun (x : int) -> x + 1`)、
letバインドされた変数 (`let x : ? = 1`) とスタンドアロンの式 (`f(x) : int`)。
型注釈のない関数のパラメータは、まだ未知のタイプで（動的型付け言語のように）動的な型として、または静的に型付けされた変数持つものとして扱われているかどうかを制御するdynamic_parameters`設定`。

We also introduce type constructor `TDynamic`, used to represent `?`.
However, `TDynamic` is only used to represent type `?` for variables in type environment; as soon as the variable is used, `TDynamic` is instantiated and replaced with a fresh *dynamic type variable*, which is represented by constructor `Unbound` but distinguished from an *ordinary type variable* with a boolean flag.
This is not unlike the treatment of polymorphic types in Algorithm W; indeed, when a variable with a polymorphic type is used, its type is instantiated by replacing all occurrences of generic type variables with fresh ordinary type variables. Conversely, just as polymorphic types can be recovered at let-bindings by generalizing free ordinary type variables, so can dynamic type variables be *frozen* at let-bindings by replacing them with `TDynamic` types (this is controlled by the setting `freeze_dynamic`).

また、型コンストラクタの`TDynamic`を導入、`?`を表すために使用されます。
しかし、`TDynamic`は、タイプが環境における変数の型TTTを表すために使用されます; すぐに変数を使用するように、`TDynamic`がインスタンス化され、新鮮な*ダイナミック型変数*に置き換えられ、ブールフラグと*通常の型変数*から`Unbound`が、著名なコンストラクタによって表されます。
これは、アルゴリズムW中の多型の型の処置と似ていなくもではありません; ポリモーフィック型の変数が使用されている場合、実際に、その型は、新鮮な通常の型変数でジェネリック型変数のすべての出現を置換することにより、インスタンス化されます。
逆に、ポリモーフィック型は自由通常タイプの変数を一般化してみましょうバインディングで回収することができるので、ダイナミック型の変数は、（これは設定`freeze_dynamicによって制御されている` TDynamic`タイプに置き換えることにより、レットバインディングで*凍結*することができます同じように`）。


The idea that makes polymorphic types polymorphic and dynamic types dynamic is that fresh type variables can be unified with any other type.
However, each type variable can only be unified once, with a single type.
This can be a problem when using functions such as `duplicate : forall[a] a -> pair[a, a]`, which duplicate type variables. The following results in an error:

アイデアは多相型の多相と動的型のダイナミックは、新鮮な型変数が他のタイプに単一化することができるということです。
しかし、それぞれの型変数は、単一のタイプで、一度単一化することができます。
型変数を複製するような`duplicate : forall[a] a -> pair[a, a]`などの関数を使用する場合に問題となり得ます。
エラーで以下の結果が得られます：

```
choose(pair([1], [true]), duplicate([]))

# ERROR: cannot unify types bool and int
```

To avoid this issue with dynamic types, we duplicate dynamic type variables after every function call, which is equivalent to first generalizing and then instantiating the result type, the trick used in `first_class_polymorphism`.

ダイナミック型でこの問題を回避するには、我々は、すべての関数呼び出し後のダイナミック型の変数を複製し、これは最初の一般化に相当し、その結果タイプをインスタンス化、`first_class_polymorphism`で使用するトリック。

```
let x : ? = 1

let y = choose(pair(1, true), duplicate(x))

# y : pair[int, bool]
```


Discussion
----------

考察
----------

This implementation focuses on gradual typing in the context of functional languages.
Similar ideas, but in a context of object-oriented languages, was explored by Jeremy Siek and Walid Taha in the paper [Gradual Typing for Objects][objects].
It was also researched by other authors, for example in [2], [3] and [4].
More recent research can be found by looking through the papers citing the above on Google Scholar. 

この実装では、関数型言語の文脈で gradual typing に焦点を当てています。
同様のアイデアは、ただしオブジェクト指向言語の文脈において、ジェレミーSiek氏とワリドタハの論文[オブジェクトの漸進的型付け][objects]によって調査されました。
また、他の著者らによって研究された例があります[2]、[3]、[4]。
より最近の研究は、Google Scholarの上の、上記引用論文を参照ください。

Although gradual type inference is not complicated, the implementation of a gradually typed language can be tricky, especially when a statically-typed function is used in dynamic code or when a dynamically-typed function is cast to a static type.
One issue is accurate reporting of runtime errors; for example, when a function `inc : int -> int` is cast to a dynamic type and called with the argument `true`, the system should report an error that the function was *called* with an argument of the wrong type.
However, if a dynamic function `fun (x : ?) -> not x` is cast to type `bool -> int` and applied to the argument `true`, the error should say that the function *returned* a value of the wrong type.
The research into the topic of correctly assigning *blame* in gradually-typed programs has culminated in the *blame theorem*, which states that "well-typed programs cannot be blamed", meaning that blame is always assigned to the dynamically-typed portion of the program.
A nice overview of this topic is provided in [5].

緩やかな型推論は複雑されていませんが、徐々に型付けされた言語の実装では、動的に型付けされた関数は、静的な型にキャストされたときに、静的に型付けされた関数は、動的コードで使用されるか、または場合は特に、注意が必要です。
1つの問題は、実行時エラーの正確な報告です。例えば、関数 `INC時：intは - > int`引数は` true`とダイナミック型にキャストと呼ばれている、システムは機能が*間違った型の引数を指定して*と呼ばれていたというエラーを報告する必要があります。
しかし、動的関数`楽しく場合（X：？）は - > BOOL`型にキャストされたX 'ではない - > int`と引数は `true`に適用される、誤差関数が*返さことを言う必要があり*誤ったの値入力します。
徐々に型付けされたプログラムで正しく割り当て*非難*のトピックの研究は、責任は常に動的に型付けされた部分に割り当てられていることを意味し、「よく型付けされたプログラムは非難することはできない」と述べている*の非難の定理*、で最高潮に達していますプログラムの。
このトピックの素晴しい概要は[5]で設けられています。

A related issue is that of efficiently translating type casts and reporting errors at the right moment.
The straightforward way of implementing function casts is to cast the argument and the return value separately: if `f` has type `? -> ?`, then the cast `f : int -> bool` can be compiled as `fun (x : int) -> (f(x) : bool)`.
However, if we have two functions that call each other recursively (e.g. the [basic example][mutual-recursion] of `is_odd` and `is_even`), one static and the other dynamic, a naive implementation would just keep adding cast upon cast, which would result in space-wise and time-wise inefficient execution.
A related issue is how to implement casts such as `((inc : int -> int) : ?) : bool -> bool` - should the type error be reported immediately, or only when the function is first used?
The first issue is touched upon in [5], while the second is elaborated more deeply in [6] and also explained in a [series of blog posts][blog-post].

関連の問題は、適切なタイミングで効率的に型キャストを翻訳し、報告するエラーのことです。
関数はキャスト実装の簡単な方法は、別々の引数と戻り値をキャストすることです：`f`が`? -> ?`型を持つ場合、その後、キャスト`f : int -> bool`は`fun (x : int) -> (f(x) : bool)`としてコンパイルすることができます。

Finally, [7] explains how to implement safe casts from dynamic functions to parametrically polymorphic types (such as `forall[a] a -> a`) by using *dynamic sealing*.
For example, if `f` has type `? -> ?`, we can implement the cast `f : forall[a] a -> a` by translating it into `fun x -> unwrap@1 (f (wrap@1 x))`, where the wrapped value `(wrap@1 x)` cannot be inspected in any way (e.g. using typecase) and can only be unwrapped by the corresponding `unwrap@1`.
This way, we can be sure that the argument `x` is really used parametrically and we can recover the proof that the only values of type `forall[a] a -> a` are the identity function and functions that either diverge or raise an error.
However, it remains unclear if this idea can be extended to polymorphic container types, i.e. how to safely implement casts such as `f : forall[a] a -> list[a]`.


References
----------

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
