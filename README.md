# type-systems-scala

Implementations of various type systems in Scala.

The original code was written in OCaml.

https://github.com/tomprimozic/type-systems.git


# オレオレ型システムを拡張しよう

# Grow Your Own Type System

<sup><sub>
contain 含む
experiment 実験
hopefully 出来るだけ
own 我々の
</sub></sup>

> <sup><sub>
*This repository* contains *implementations of different type systems* {in Scala}.

このリポジトリには、Scalaで書いた異なるタイプシステムの実装が含まれています。

> <sup><sub>
{It is meant to help out anyone} <- who {wants to learn more about<- *advanced type systems and type inference* or experiment {by {extending or implementing} their own}.
*The implementations* {are minimal} and {contain {code that is *(hopefully) simple and clear* }}.

これは、拡張または独自に実装することで、高度な型システムと型推論や実験についてもっと学びたいという方を助けることを意味しています。
実装は最小限で、（出来るだけ）単純明快であるコードが含まれています。

<sup><sub>
yet かつ、だからといって (andみたいなもの)
efficient 効率的な
such as のような
substitution 代入、置換
assign 割り当てる
generalization 一般化
</sub></sup>

-   > <sup><sub>
    [**algorithm_w**](https://github.com/hsk/type-systems-scala/tree/master/algorithm_w)
    contains *one of the most basic* yet *efficient implementation* of *Damas-Hindley-Milner type inference algorithm*
    ({used in} *functional languages* {such as} *OCaml, Haskell and Elm*) called *Algorithm W*.
    *Uses references to simulate type substitutions* and *assigns ranks/levels to type variables* {to simplify *let-generalization*}.
    
    [**algorithm_w**](https://github.com/hsk/type-systems-scala/tree/master/algorithm_w)には
    *Algorithm W* と呼ばれる（例えばOCaml、HaskellとElmのような関数型言語で使用されている）Damas-Hindley-Milner型推論アルゴリズムの最も基本的で効率的な実装が含まれています。
    型置換をシミュレートするために参照を使用し、let一般化を簡素化するために型変数にランク/レベルを割り当てます。

<sup><sub>
extensible 拡張可能な
excellent 素晴しい
although だけれども、しかし、であるが
assign 割り当てる
generalization 一般化
extremly 非常に
surprisingly 驚くほど
incorporated 組み込まれる
</sub></sup>

-   > <sup><sub>
    [**extensible_rows**](https://github.com/hsk/type-systems-scala/tree/master/extensible_rows)
    extends **algorithm_w** with type inference for extensible records/rows
    with scoped labels, based on Daan Leijen's excellent [paper][extensible_rows]. Although
    this is just one way of implementing extensible records, it's extremly simple and
    surprisingly useful, and was incorporated into the programming language
    [Elm](http://elm-lang.org/learn/Records.elm).
	
    [**extensible_rows**](https://github.com/hsk/type-systems-scala/tree/master/extensible_rows)
    はDaan Leijenの素晴らしい[論文][extensible_rows]に基づいて、スコープ付きラベルと拡張可能なレコード/行の型推論で **algorithm_w** を拡張します。
	これは拡張可能なレコードを実現するためのひとつの方法ですが、それは非常に簡単で驚くほど便利であり、[Elm](http://elm-lang.org/learn/Records.elm)プログラミング言語に組み込まれました。

-   > <sup><sub>
    [**extensible_rows2**](https://github.com/hsk/type-systems-scala/tree/master/extensible_rows2)
    is an optimized implementation of **extensible_rows**.
    
    [**extensible_rows2**](https://github.com/hsk/type-systems-scala/tree/master/extensible_rows2)
    は**extensible_rows** の最適化された実装です。

<sup><sub>
partial 部分的、パーシャル
higher-rank 高階
polymorphism 多相
slightly わずかに
attempt こころみ
considerably かなり
</sub></sup>

-   > <sup><sub>
    [**first_class_polymorphism**](https://github.com/hsk/type-systems-scala/tree/master/first_class_polymorphism)
    extends **algorithm_w** with type checking and partial type inference for first-class
    and higher-rank polymorphism, based on another one of Daan Leijen's [papers][hmf].
    This system requires slightly more type annotations than other attempts at type inference for
    first-class polymorphism, such as ML<sup>F</sup>, but is considerably simpler to implements
    and use.

    [**first_class_polymorphism**](https://github.com/hsk/type-systems-scala/tree/master/first_class_polymorphism)は
    Daan Leijenの[論文][hmf]の他の一方に基づいてファーストクラスおよび高階多相型の型チェックと部分的な型推論で、 **algorithm_w** を拡張します。
    このシステムは、ML<sup>F</sup>などのファーストクラスの多型のための型推論で他の試みよりもわずかに型注釈を必要としますが、実装および使い方はかなり簡単です。

<sup><sub>
gradual 段階的、漸進的、なめらかな
combine 結合する、合体する、併せ持つ
benefits 利点
safer より安全な
flexible 柔軟な
delaing 遅延
until まで、かけて
neccessary 必要
</sub></sup>

-   > <sup><sub>
    [**gradual_typing**](https://github.com/hsk/type-systems-scala/tree/master/gradual_typing)
    is another simple extension of **algorithm_w** based on a [paper][gradual] by Jeremy G. Siek
    and Manish Vachharajani. Gradual typing combines the benefits of static and dynamic typing,
    allowing programmers to make dynamic programs safer by adding static type information, and
    make static programs more flexible by delaying type-checking until runtime when necessary.
    
    [**gradual_typing**](https://github.com/hsk/type-systems-scala/tree/master/gradual_typing)
    は Jeremy G. Siek と Manish Vachharajani による[論文][gradual]に基づいた、**algorithm_w**の別の単純な拡張です。
    gradual typing(漸進的型付け)は、プログラマが、静的な型の情報を追加することにより、動的なプログラムをより安全に、必要に応じて実行時まで型チェックを遅延させることにより、静的なプログラムをより柔軟にできるように、静的および動的型付けの利点を兼ね備えています。

<sup><sub>
experiment 実験
dependent 依存
contracts 契約
external automatic theorem prover 外部の自動定理証明
verify 確認
satisfied 満足、満たす
prevent 未然に防ぐ、防止する
</sub></sup>

-   > <sup><sub>
    [**refined_types**](https://github.com/hsk/type-systems-scala/tree/master/refined_types)
    is an experiment that extends the HM type system with dependent types in the form of function
    contracts. It uses an external automatic theorem prover to verify that function contracts are
    satisfied, to prevent many of the most common software errors, such
    as division by zero and out-of-bounds array access.

    [**refined_types**](https://github.com/hsk/type-systems-scala/tree/master/refined_types)は契約関数の形の依存型で、HM型システムを拡張する実験です。
    これは、配列の範囲外アクセスやゼロの除算などの最も一般的なソフトウェアのエラーの多くを防止するために、契約が満たされたその関数を確認するために、外部の自動定理証明を使用しています。


[extensible_rows]: http://research.microsoft.com/apps/pubs/default.aspx?id=65409
[hmf]: http://research.microsoft.com/apps/pubs/default.aspx?id=132621
[gradual]: http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.84.8219&rep=rep1&type=pdf
