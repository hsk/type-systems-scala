# Algorithm W

Algorithm W の適当な訳

|English|日本語|
| --- | --- |
|infer|推論する|
|occur|出現する|
|unify|単一化|
|polymorphism|多相性|
|porimorphic|多相的|
|substitution|代入、置換|
|generalize|一般化|
|instantiate|具体化|

|English|日本語|
| --- | --- |
|principal|主要な|
|although|であるが　だが|
|explicit|明確な、厳格な、自明の|
|substitution|代入、置換|
|permit|許可する,可能にする|
|efficient|効率的な|
|achieving|達成する|

Algorithm W is the original algorithm for infering types in the Damas-Hindley-Milner type system.
It supports polymorphic types, such as `forall[a] a -> a`, let generalization, and infers principal (most general) types.
Although it is formally described using explicit substitutions, it permits an efficient implemenation using updatable references, achieving close to linear time complexity (in terms of the size of the expression being type-infered).

アルゴリズムWはダマ・ヒンドリー - ミルナー型システムの型を推論するための独自のアルゴリズムです。
`forall[a] -> a` のような多相型をサポートし、let一般化をし、主要な（最も一般的な）型を推論します。
アルゴリズムWは形式的には明示的な代入を使用して記述されますが、更新可能な参照を使用して効率的な実装を可能にして、
（型推論する項の式のサイズの点で）線形時間オーダーの計算量を達成しています。

## Overview

## 概要

|English|日本語|
| --- | --- |
|several|いくつかの a few < several < many|
|naive|素朴な,単純な|
|insted|その代りに|
|bound|束縛|
|unbound|未束縛|
|eloquent|能弁だ；爽やかだ；爽かだ；宛転たる；能辯だ；雄弁だ；さわやかだ|


For a general description of Algorithm W, see the [Wikipedia article][wikipedia].
This implementation uses several optimizations over the naive implementation.
Instead of explicit substitutions when unifying types, it uses updateable references.
It also tags unbound type variables with levels or ranks to optimize generalizations of let bindings, a technique first described by Didier Rémy [1].
A very eloquent description of the ranked type variables algorithm and associated optimizations was written by [Oleg Kiselyov][oleg].

アルゴリズムWの一般的な説明については、[Wikipediaの記事][wikipedia]を参照してください。
この実装は、素朴な実装に優るいくつかの最適化を使用しています。
型を単一化するときに明示的な置換の代わりに、更新可能な参照を使用しています。
また、レベルと未束縛な型変数にタグを付けるかのletバインディングの一般化を最適化するためにランクされていて、このテクニックはディディエ・レミーによって最初に記載されました [1]。
非常に説得力のあるランク付き型変数のアルゴリズムと関連する最適化の説明は、[Oleg Kiselyov][oleg]によって書かれました。

## Details

## 詳細

The basic terms of Lambda Calculus and the structure of the types are defined in `expr.ml`.
Lexer is implemented in `lexer.mll` using `ocamllex`, and a simple parser in file `parser.mly` using `ocamlyacc`.
The main type inference is implemented in the file `infer.ml`.

ラムダ計算の基本的な項と型の構造は `expr.ml` で定義されています。
字句解析器はocamllexを使用して`lexer.mll`で実装され、単純なパーサーは`ocamlyacc`を使用したファイル `parser.mly`です。
メインの型推論は、ファイル `infer.ml`で実装されています。

The function `infer` takes an environment, a level used for let-generalization, and an expression, and infers types for each term type.
*Variables* are looked up in the environment and instantiated.
The type of *functions* is inferred by adding the function parameters to the type environment using fresh type variables, and inferring the type of the function body.
The type of *let* expression is inferred by first inferring the type of the let-bound value, generalizing the type, and the inferring the type of the let body in the extended type environment.
Finally, the type of a *call* expression is inferred by first matching the type of the expression being called using the `match_fun_ty` function, and then inferring the types of the arguments and unifying them with the types of function parameters.

`infer` 関数は、環境、let一般化するために使用されるレベル、および式をとり、それぞれの項の型を型推論します。
変数は、環境内で検索され、インスタンス化されます。
関数の型は、新たな型の変数を使用して型環境に関数パラメータを追加し、関数本体の型を推論することで推論されます。
*let*式の型は最初にletの値の型を推論し、次に型の一般化し、そして拡張された型環境でlet本体の型を推論することによって推論されます。
最後、*call* 式の型は、最初の`match_fun_ty`関数を使用して呼び出されている式の型にマッチした後、引数の型を推論し、関数パラメータの型とそれらを単一化することにより推論されます。

|English|日本語|
| --- | --- |
|i.e.|つまり、すなわち|
|determine|決定|
|identical|同一の|

The function `unify` takes two types and tries to *unify* them, i.e. determine if they can be equal.
Type constants unify with identical type constants, and arrow types and other structured types are unified by unifying each of their components.
After first performing an "occurs check", unbound type variables can be unified with any type by replacing their reference with a link pointing to the other type.

`unify` 関数は2つの型を取りそれらを*unify*しようとします、すなわち、引数が等しくできるか判断します。
入力定数は同じ型定数を単一化し、タイプを矢印やその他の構造化タイプは、各コンポーネントを統合して単一化されています。
最初の実行後、非結合型の変数は、他のタイプを指すリンクとの参照を置き換えることにより、任意のタイプで単一化することができ、「チェック起こります」。
型定数は同じ型定数を単一化し、型を矢印やその他の構造化型は、各成分を単一化することで単一化されています。
最初の"出現チェック"の実行後、未結合の型変数は、他の型を指すリンクとの参照を置き換えることにより、任意の型で単一化することができます。

|English|日本語|
| --- | --- |
|adjust|整える|
|diverge|発散する、分岐する|
|ensuring|確保する|
|correctly|正しく|

The function `occurs_check_adjust_levels` makes sure that the type variable being unified doesn't occur within the type it is being unified with.
This prevents the algorithm from inferring recursive types, which could cause naively-implemented type checking to diverge.
While traversing the type tree, this function also takes care of updating the levels of the type variables appearing within the type, thus ensuring the type will be correctly generalized.

`occurs_check_adjust_levels`関数は、型変数は、それが単一化されている型内で出現しないで単一化されていることを確認します。
これは発散するチェック素朴に実装型を引き起こす可能性が再帰的な型を推論からアルゴリズムを防ぐことができます。
タイプ・ツリーをトラバースしながら、この関数は、このように正確に一般化される型を確保し、型内に現れる型変数のレベルを更新するの面倒を見ます。

Function `generalize` takes a level and a type and turns all type variables within the type that have level higher than the input level into generalized (polymorphic) type variables.
Function `instantiate` duplicates the input type, transforming any polymorphic variables into normal unbound type variables.

`generalize`(一般化)関数は、レベルや型を取り、一般化（多相型）への入力レベルよりも高いレベルを持っている型内ですべての型変数の変数を入力をオンにします。
`instantiate`(インスタンス化)関数は、通常未結合の型の変数に任意の多型の変数を変換する、入力タイプが複製されます。

## Possible optimizations

## 可能な最適化

|English|日本語|
| --- | --- |
|avoid|避ける|
|for the sake of|のために|
|during|中に|
|adjustment|調整、補正|
|deal|取引、契約|
|arise|生じる、起こる、発生する|
|further|さらに|
|condensing|凝縮します|
|take care of|面倒を見る|

Although this implementation is reasonably efficient, state-of-the-art implementations of HM type inference employ several more advanced techniques which were avoided in this implementation for the sake of clarity.
As outlined in Oleg's article, OCaml's type checker marks *every* type with a type level, which is the maximum type level of the type variables occuring within it, to avoid traversing non-polymorphic types during instantiation.
It also delays occurs checks and level adjustments.
It can deal with recursive types, which arise often while type checking objects, by marking types during unification.
Oleg describes a further optimization that could be performed by condensing the sequences of type links as we follow them, but our `generalize` function takes care of that problem.

この実装は適度に効率的であるが、HM型推論の最先端の実装では、明確にするために、この実装では回避されたいくつかのより高度なテクニックを使用します。
オレグの記事で概説してあるように、OCamlでの型チェッカーは、インスタンス化中に非多型型を横断避けるために、その中に起きて型変数の最大型レベルで型レベル、とのすべてのタイプをマーク。
また、遅延がチェックおよびレベル調整を発生します。
これは、タイプが統一時に型をマークすることによって、オブジェクトを確認しながら、多くの場合、発生する再帰的な型を扱うことができます。
オレグは、我々はそれに従うようなタイプのリンクのシーケンスを凝縮させることによって実施することができる更なる最適化を説明していますが、私たちのgeneralize関数は、その問題の面倒を見ます。

## References

## 参考文献

[1] Didier Rémy. *Extending ML type system with a sorted equational theory.* 1992


[wikipedia]: http://en.wikipedia.org/wiki/Hindley%E2%80%93Milner_type_system#Algorithm_W
[oleg]: http://okmij.org/ftp/ML/generalization.html


-----


ここにあるScalaに移植したものです。


    sbt ~run

で実行します。

- ソースを見て理解を深める


これは、OCamlの多相的な型推論のレベルを使った高速化されているものです。
重要なのは、infer.scalaですので、infer.scalaを見てみましょう。

    object Infer {

      import Expr._
      val current_id = Ref(0)

      def next_id():Int = {
        val id = current_id.a
        current_id.a = id + 1
        id
      }

      def reset_id() {
        current_id.a = 0
      }

      def new_var(level:level):Ty = TVar(Ref(Unbound(next_id(), level)))

      def new_gen_var():Ty = TVar(Ref(Generic(next_id())))

      def error(msg:String) { throw new Exception(msg) }

      object Env {...}

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {...}
      def iter2[A,B](a:List[A],b:List[B],f:(A,B)=>Unit) {...}
      def unify(ty1:Ty, ty2:Ty) {...}
      def generalize(level:level, ty:Ty):Ty = {...}
      def instantiate(level:level, ty:Ty):Ty = {...}
      def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {...}
      def infer(env:Env.env, level:level, expr:Expr):Ty = {...}

    }

next\_id, reset\_id は新しいIDを作ったり、IDのリセットを行います。
new\_var は新しい変数、new\_gen_varはGenericな新しい変数。errorはエラー関数。
object Envは環境。
occurs\_check\_adjust\_levelsは出現チェック
iter2は2つのリストのイテレート。
unifyは単一化
generalizeが一般化
intantiateがインスタンス化
match\_fun\_tyが関数の型のマッチング
inferが型推論のエントリポイントになります。

OCamlからの移植なので、プログラムは上からボトムアップ的に書かれています。トップダウン的に見たければ、最後の関数から読んで行くと良いでしょう。

ここに来るまでの詳しい話は、[oleg](http://okmij.org/ftp/ML/generalization.html)が詳しいです。




## EBNF

    ident       ::= [_A-Za-z][_A-Za-z0-9]*
    integer     ::= [0-9]+

    expr        ::= app_expr
                  | "let" ident "=" expr "in" expr
                  | "fun" rep1(ident) "->" expr

    app_expr    ::= simple_expr rep("(" rep1sep(expr, ",") ")")

    simple_expr ::= ident
                  | "(" expr ")"

    ty_forall   ::= ty
                  | "forall" "[" rep1(ident) "]" ty

    ty          ::= app_ty "->" ty
                  | app_ty
                  | "(" repsep(ty, ",") ")" "->" ty

    app_ty      ::= simple_ty rep("[" rep1sep(ty, ",") "]")

    simple_ty   ::= ident
                  | "(" ty ")"
