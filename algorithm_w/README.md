> <sup><sub>
Algorithm W

# Algorithm W

<sup><sub>
infer 推論する
occur 出現する
unify 単一化
polymorphism 多相性
polymorphic 多相的
substitution 代入、置換
generalize 一般化
instantiate 具体化
</sub></sup>

<sup><sub>
principal 主要な
although ですが、だが
explicit 明確な、厳格な、自明の
substitution 代入、置換
permit 許可する,可能にする
efficient 効率的な
achieving 達成する
</sub></sup>

> <sup><sub>
Algorithm W is the original algorithm for infering types in the Damas-Hindley-Milner type system.
It supports polymorphic types, such as `forall[a] a -> a`, let generalization, and infers principal (most general) types.
Although it is formally described using explicit substitutions, it permits an efficient implemenation using updatable references, achieving close to linear time complexity (in terms of the size of the expression being type-infered).

アルゴリズムWは Damas-Hindley-Milner 型システムの型のオリジナルの推論アルゴリズムです。
`forall[a] -> a` のような多相型をサポートし、let一般化を行い、主要な(最も一般的な)型を推論します。
アルゴリズムWは形式的には明示的な代入を使用して記述されていますが、ここでは更新可能な変数を使って効率的な実装を可能とし、
(型推論する項の式のサイズに応じて)線形時間オーダーの計算量を達成しています。

> <sup><sub>
Overview

## 概要

<sup><sub>
several いくつかの a few < several < many
naive 素朴な,単純な
insted その代りに
bound 束縛
unbound 未束縛
eloquent 能弁だ；爽やかだ；爽かだ；宛転たる；能辯だ；雄弁だ；さわやかだ
</sub></sup>

> <sup><sub>
For a general description of Algorithm W, see the [Wikipedia article][wikipedia].
This implementation uses several optimizations over the naive implementation.
Instead of explicit substitutions when unifying types, it uses updateable references.
It also tags unbound type variables with levels or ranks to optimize generalizations of let bindings, a technique first described by Didier Rémy [1].
A very eloquent description of the ranked type variables algorithm and associated optimizations was written by [Oleg Kiselyov][oleg].

アルゴリズムWの一般的な説明は、[Wikipediaの記事][wikipedia]を参照してください。
この実装は、素朴な実装に優るいくつかの最適化を使用しています。
型を単一化するときに明示的な置換の代わりに、更新可能な参照を使用しています。
それは、未束縛な型変数でlet束縛の一般化を最適化するためlevelまたはrankと一緒にタグ付けされます。この技術はDidier Rémyによって最初に記載されました [1]。
非常に雄弁なランク付き型変数のアルゴリズムと関連する最適化の説明は、[Oleg Kiselyov][oleg]によって書かれました。

> <sup><sub>
Details

## 詳細

> <sup><sub>
The basic terms of Lambda Calculus and the structure of the types are defined in `expr.ml`.
Lexer is implemented in `lexer.mll` using `ocamllex`, and a simple parser in file `parser.mly` using `ocamlyacc`.
The main type inference is implemented in the file `infer.ml`.

ラムダ計算の基本的な項と型の構造は `expr.ml` で定義されています。
字句解析器はocamllexを使用して`lexer.mll`で、単純なパーサーは`ocamlyacc`を使用してファイル `parser.mly`で実装されています。
メインの型推論はファイル `infer.ml`で実装されています。

> <sup><sub>
The function `infer` takes an environment, a level used for let-generalization, and an expression, and infers types for each term type.

`infer` 関数は、環境(env)、let一般化で使われるレベル(level)、および式(expr)をとり、それぞれの項の型を型推論します。

    def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
      expr match {

> <sup><sub>
*Variables* are _looked up in the environment and instantiated_.

*変数* は、環境内で検索され、インスタンス化されます。

        case Var(name) =>
          try {
            instantiate(level, env(name))
          } catch {
            case _:Throwable => error("variable " + name + " not found")
          }

> <sup><sub>
The type of *functions* is inferred by _adding the function parameters to the type environment using fresh type variables_, and inferring _the type of the function body_.

*関数* は、新たな型変数を使用して型環境に関数パラメータを追加し、関数本体の型を推論します。

        case Fun(param_list, body_expr) =>
          val param_ty_list = param_list.map{ _ => new_var(level)}
          val fn_env =
            param_list.zip(param_ty_list).foldLeft(env) {
              case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
            }
          val return_ty = infer(fn_env, level, body_expr)
          TArrow(param_ty_list, return_ty)

> <sup><sub>
_The type of *let* expression_ is inferred by _first inferring the type of the let-bound value_, generalizing the type, and _the inferring the type of the let body in the extended type environment_.

*let* 式は最初にletの値の型を推論し、型を一般化し、そして拡張された型環境でlet本体の型を推論します。

        case Let(var_name, value_expr, body_expr) =>
          val var_ty = infer(env, level + 1, value_expr)
          val generalized_ty = generalize(level, var_ty)
          infer (env + (var_name -> generalized_ty), level, body_expr)

> <sup><sub>
Finally, the type of a *call* expression is inferred by first matching the type of the expression being called using the `match_fun_ty` function, and then inferring the types of the arguments and unifying them with the types of function parameters.

最後、*call* 式は、最初に`match_fun_ty`関数で呼び出される関数式の型にマッチさせ、引数の型を推論し、関数パラメータの型と単一化します。

        case Call(fn_expr, arg_list) =>
          val (param_ty_list, return_ty) =
            match_fun_ty(arg_list.length, infer(env, level, fn_expr))
          param_ty_list.zip(arg_list).foreach{
            case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
          } 
          return_ty
      }
    }

<sup><sub>
i.e. つまり、すなわち
determine 決定
identical 同一の
</sub></sup>

> <sup><sub>
_The function `unify`_ takes _two types_ and tries to *unify* _them_, i.e. determine if _they_ can be _equal_.

`unify` 関数は2つの型を取りそれらの*単一化*を試みます。すなわち、引数が等しくできるかを判断します。

    def unify(ty1:Ty, ty2:Ty) {
      if (ty1 == ty2) return
      (ty1, ty2) match {

> <sup><sub>
_Type constants_ unify with _identical type constants_, and _arrow types and other structured types_ are unified by _unifying each of their components_.

型定数の単一化は同じ型定数を単一化し、Arrow型やその他の構造化型は、各要素をそれぞれ単一化します。

        case (TConst(name1), TConst(name2)) if(name1 == name2) =>
        case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
            ty_arg_list1.zip(ty_arg_list2).foreach{
              case (a,b) => unify(a, b)
            }
        case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
            param_ty_list1.zip(param_ty_list2).foreach{
              case(a,b) => unify(a, b)
            }
            unify(return_ty1, return_ty2)
        case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
        case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
        case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
            assert(false) // There is only a single instance of a particular type variable.

> <sup><sub>
After first performing an "occurs check", unbound type variables can be unified with any type by replacing their reference with a link pointing to the other type.

最初の"出現チェック"の実行後、未結合(Unbound)の型変数は、他の型を指すLinkに変数を置き換えることにより、任意の型で単一化することができます。

        case (tvar @ TVar(Unbound(id, level)), ty) =>
            occurs_check_adjust_levels(id, level, ty)
            tvar.a = Link(ty)
        case (ty, tvar @ TVar(Unbound(id, level))) =>
            occurs_check_adjust_levels(id, level, ty)
            tvar.a = Link(ty)
        case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
      }
    }

<sup><sub>
adjust 整える
diverge 発散する、分岐する
ensuring 確保する
correctly 正しく
</sub></sup>

> <sup><sub>
The function `occurs_check_adjust_levels` makes sure that the type variable being unified doesn't occur within the type it is being unified with.
_This_ prevents _the algorithm_ from _inferring recursive types_, which could cause naively-implemented type checking to diverge.
While traversing the type tree, this function also takes care of updating the levels of the type variables appearing within the type, thus ensuring the type will be correctly generalized.

`occurs_check_adjust_levels`関数は、型変数が単一化されている型内で出現せずに単一化されることを確認します。
この関数を使えば、再帰的型推論アルゴリズムの発散を防止出来ます。
この関数は、型の構文木をトラバースしながら、正確に一般化される型を確保し、場合によっては型内に現れる型変数のレベルの更新をします。

    def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
      def f(ty:Ty) {
        ty match {
          case TVar(Link(ty)) => f(ty)
          case TVar(Generic(_)) => assert(false)
          case other_tvar @ TVar(Unbound(other_id, other_level)) =>
            if (other_id == tvar_id) error("recursive types")          
            if (other_level > tvar_level)
              other_tvar.a = Unbound(other_id, tvar_level)
          case TApp(ty, ty_arg_list) =>
            f(ty)
            ty_arg_list.foreach(f)
          case TArrow(param_ty_list, return_ty) =>
            param_ty_list.foreach(f)
            f(return_ty)
          case TConst(_) =>
        }
      }
      f(ty)
    }

> <sup><sub>
_Function `generalize`_ takes _a level and a type_ and turns _all type variables within the type_ that have _level higher_ than _the input level into generalized (polymorphic) type variables_.

`generalize`(一般化)関数は、レベルと型を取り、一般化された（多相的）型変数の入力レベルよりも高いレベルの型内ですべての型変数を一般化します。

    def generalize(level:level, ty:Ty):Ty = {
      ty match {
        case TVar(Unbound(id, other_level)) if other_level > level =>
          TVar(Generic(id))
        case TApp(ty, ty_arg_list) =>
          TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
        case TArrow(param_ty_list, return_ty) =>
          TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
        case TVar(Link(ty)) => generalize(level, ty)
        case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
      }
    }

> <sup><sub>
_Function `instantiate`_ duplicates _the input type_, transforming _any polymorphic variables into normal unbound type variables_.

`instantiate`(インスタンス化)関数は入力した型を複製し、_通常未結合の型の変数に任意の多相型の変数_を変換します。

    def instantiate(level:level, ty:Ty):Ty = {
      var id_var_map = Map[id,Ty]()
      def f (ty:Ty):Ty = {
        ty match {
          case TConst(_) => ty
          case TVar(Link(ty)) => f(ty)
          case TVar(Generic(id)) =>
            id_var_map.get(id) match {
              case Some(a) => a
              case None =>
                val var1 = new_var(level)
                id_var_map = id_var_map + (id -> var1)
                var1
            }
          case TVar(Unbound(_,_)) => ty
          case TApp(ty, ty_arg_list) =>
            TApp(f(ty), ty_arg_list.map(f))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(f), f(return_ty))
        }
      }
      f(ty)
    }

> <sup><sub>
Possible optimizations

## 可能な最適化

<sup><sub>
state-of-the-art 最先端
avoid 避ける
for the sake of のために
during 中に
adjustment 調整、補正
deal 取引、契約
arise 生じる、起こる、発生する
further さらに
condensing 凝縮します
take care of 気にかける
</sub></sup>

> <sup><sub>
Although _this implementation_ is _reasonably efficient_, _state-of-the-art implementations of HM type inference_ employ _several more advanced techniques_ which were avoided in _this implementation for the sake of clarity_.
As _outlined in Oleg's article_, _OCaml's type checker_ marks _*every* type with a type level_, which is _the maximum type level of the type variables occuring within it_, to avoid traversing _non-polymorphic types during instantiation_.
It also delays occurs checks and level adjustments.
It can deal with recursive types, which arise often while type checking objects, by marking types during unification.
_Oleg_ describes _a further optimization_ that could be performed by _condensing the sequences of type links as we follow them_, but _our `generalize` function_ takes care of _that problem_.

この実装は適度に効率的ですが、HM型推論の最先端の実装では、明確にするためにこの実装で回避されたいくつかのより高度なテクニックを使用します。
Olegの記事で概説してあるように、OCamlの型検査器は、インスタンス化中に非多相型の横断を避けるために、その中に現れる型変数の最大型レベルでの型レベルのすべての型をマークします。
また、出現チェックとレベル調整を遅延させます。
これは、オブジェクトを確認しながら、型の単一化時に型をマークし、何度も現れる再帰的な型を扱うことができます。
Olegは、我々がそれに従うような型のリンクのシーケンスを凝縮させることによって実施することができる更なる最適化を説明していますが、我々のgeneralize関数は、その問題が気になります。


> <sup><sub>
References

## 参考文献

[1] Didier Rémy. *Extending ML type system with a sorted equational theory.* 1992


[wikipedia]: http://en.wikipedia.org/wiki/Hindley%E2%80%93Milner_type_system#Algorithm_W
[oleg]: http://okmij.org/ftp/ML/generalization.html


-----

# 訳者による追記


ここにあるソースはオリジナルのOCamlのコードをScalaに移植したものです。

## 実行方法

    sbt ~run

で実行できます。テストはただのメイン関数内にあります。


## 解説

これは、OCamlの多相的な型推論をレベルを使って高速化したプログラムの簡単な例です。

メインの処理は、infer.scalaにあるので、infer.scalaをもう一度見てみましょう。

    package dhm

    object Infer {

      import Expr._

next\_id, reset\_id は新しいIDを作ったり、IDのリセットを行います。

      var current_id = 0

      def next_id():Int = {
        val id = current_id
        current_id = id + 1
        id
      }

      def reset_id() {
        current_id = 0
      }

これは次の型変数を作成するときに使われます。

new\_var は新しい未束縛の型変数、new\_gen_varはGenericな新しい型変数をつくります。errorはエラー関数です。

      def new_var(level:level):Ty = TVar(Unbound(next_id(), level))

      def new_gen_var():Ty = TVar(Generic(next_id()))

      def error(msg:String):Nothing = throw new Exception(msg)

occurs\_check\_adjust\_levelsは再帰的な型の出現チェックを行うことで、無限ループを防ぎます。

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
        def f(ty:Ty) {
          ty match {
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(_)) => assert(false)
            case other_tvar @ TVar(Unbound(other_id, other_level)) =>
              if (other_id == tvar_id) error("recursive types")          
              if (other_level > tvar_level)
                other_tvar.a = Unbound(other_id, tvar_level)
            case TApp(ty, ty_arg_list) =>
              f(ty)
              ty_arg_list.foreach(f)
            case TArrow(param_ty_list, return_ty) =>
              param_ty_list.foreach(f)
              f(return_ty)
            case TConst(_) =>
          }
        }
        f(ty)
      }

unifyは型２つを受け取って、型同士が同じになるように調整します。単一化と言います。

      def unify(ty1:Ty, ty2:Ty) {
        if (ty1 == ty2) return
        (ty1, ty2) match {
          case (TConst(name1), TConst(name2)) if(name1 == name2) =>
          case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
              ty_arg_list1.zip(ty_arg_list2).foreach{
                case (a,b) => unify(a, b)
              }
          case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
              param_ty_list1.zip(param_ty_list2).foreach{
                case(a,b) => unify(a, b)
              }
              unify(return_ty1, return_ty2)
          case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
          case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
          case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
              assert(false) // There is only a single instance of a particular type variable.
          case (tvar @ TVar(Unbound(id, level)), ty) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
          case (ty, tvar @ TVar(Unbound(id, level))) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

generalizeは変数が定義されたときに呼び出され、値が多相的だった場合に、一般化された型パラメータに置き換えます。

      def generalize(level:level, ty:Ty):Ty = {
        ty match {
          case TVar(Unbound(id, other_level)) if other_level > level =>
            TVar(Generic(id))
          case TApp(ty, ty_arg_list) =>
            TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
          case TVar(Link(ty)) => generalize(level, ty)
          case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
        }
      }

intantiateは変数が現れた場合に呼び出され、一般化された型パラメータを逆に具体化します。

      def instantiate(level:level, ty:Ty):Ty = {
        var id_var_map = Map[id,Ty]()
        def f (ty:Ty):Ty = {
          ty match {
            case TConst(_) => ty
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(id)) =>
              id_var_map.get(id) match {
                case Some(a) => a
                case None =>
                  val var1 = new_var(level)
                  id_var_map = id_var_map + (id -> var1)
                  var1
              }
            case TVar(Unbound(_,_)) => ty
            case TApp(ty, ty_arg_list) =>
              TApp(f(ty), ty_arg_list.map(f))
            case TArrow(param_ty_list, return_ty) =>
              TArrow(param_ty_list.map(f), f(return_ty))
          }
        }
        f(ty)
      }

match\_fun\_tyは関数の型のマッチングを行うinfer関数の補助関数です。

      def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
        ty match {
          case TArrow(param_ty_list, return_ty) =>
            if (param_ty_list.length != num_params)
              error("unexpected number of arguments")
            (param_ty_list, return_ty)
          case TVar(Link(ty)) => match_fun_ty(num_params, ty)
          case tvar@TVar(Unbound(id, level)) =>
            val param_ty_list = List.fill(num_params){new_var(level)}
            val return_ty = new_var(level)
            tvar.a = Link(TArrow(param_ty_list, return_ty))
            (param_ty_list, return_ty)
          case _ => error("expected a function")
        }
      }

inferが式を受け取り型を推論する型推論のエントリポイントです。

      def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
        expr match {
          case Var(name) =>
            try {
              instantiate(level, env(name))
            } catch {
              case _:Throwable => error("variable " + name + " not found")
            }
          case Fun(param_list, body_expr) =>
            val param_ty_list = param_list.map{ _ => new_var(level)}
            val fn_env =
              param_list.zip(param_ty_list).foldLeft(env) {
                case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
              }
            val return_ty = infer(fn_env, level, body_expr)
            TArrow(param_ty_list, return_ty)
          case Let(var_name, value_expr, body_expr) =>
            val var_ty = infer(env, level + 1, value_expr)
            val generalized_ty = generalize(level, var_ty)
            infer (env + (var_name -> generalized_ty), level, body_expr)
          case Call(fn_expr, arg_list) =>
            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{
              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }


OCamlからの移植なので、プログラムは上からボトムアップ的に書かれています。トップダウン的に見たければ、最後の関数から読んで行くと良いでしょう。

このアルゴリズムは大ざっぱに言うと、式を`infer`で推論して、２つの型を`unify`で同じ物であるとする単一化を行います。
`unify`をする際に、再帰的に現れる型があると固まってしまうので出現チェックが必要になります。そこで`occurs_check_adjust_levels`で調べます。
多相的な関数を扱うには、letバインドがあった場合に`generalize`で一般化して、変数の参照があった場合に`instantiate`で具体化する事が必要です。
`infer`関数内の関数呼び出しは複雑になるので補助関数の、`match_fun_ty`を使って引数と戻り値の処理をしています。

## 最適化について

このアルゴリズムでは２つの最適化技法を使っています。

１つ目は、書き換え可能な変数を使うことで、代入の操作を高速化しています。
型推論は、型の方程式を作って分からない型へ分かっている型を代入する事で推論できるのですが、代入の式をたくさん作って後で計算するよりもその場で代入操作をしてしまったほうが速い訳です。破壊的な変更が必要になりますが、コンパイルは高速なほうがいいですよね。これは単相の型推論を高速化するアルゴリズムです。

２つ目は、レベルを使った多相的な型推論の最適化です。

多相的な型推論をするには、通常型スキームと呼ばれる、テンプレートの集合のような物を使います。
アルゴリズムWの多相的な一般化するアルゴリズムは基本的に遅く、Olegの解説によると

https://github.com/hsk/docs/blob/master/generalization/2.%E4%B8%80%E8%88%AC%E5%8C%96.md

レミーはCamlのコンパイルは非常に重くて20分かかったので、もっと速くしたかったそうです。

そこで多相的な一般化も書き換え可能な変数を使って高速したアイディアが生まれます。

とりあえず作ってみた例が3.メモリ管理ミスがある不完全な一般化です。

https://github.com/hsk/docs/blob/master/generalization/3.%E3%83%A1%E3%83%A2%E3%83%AA%E7%AE%A1%E7%90%86%E3%83%9F%E3%82%B9%E3%81%8C%E3%81%82%E3%82%8B%E4%B8%8D%E5%AE%8C%E5%85%A8%E3%81%AA%E4%B8%80%E8%88%AC%E5%8C%96.md

残念ながら、単純に多相的な物に適用しようとすると無理があったので、例えば以下のような推論をしてします。

    fun x y -> let x = x y in x y : (b -> c) -> b -> e


そこで、レベルの考え方を導入したのが、4.レベルによる効率的な一般化です。

https://github.com/hsk/docs/blob/master/generalization/4.%E3%83%AC%E3%83%99%E3%83%AB%E3%81%AB%E3%82%88%E3%82%8B%E5%8A%B9%E7%8E%87%E7%9A%84%E3%81%AA%E4%B8%80%E8%88%AC%E5%8C%96.md

多相的な関数の推論にスコープ的な要素を加えて完全な物としたので以下のように推論されます。

    fun x y -> let x = x y in x y : (b -> b -> d) -> b -> d

## レベルについて

以下の式をレベルを意識せずに推論する事を考えましょう。

    fun x y -> let z = x y in z y : (b -> b -> d) -> b -> d

すると以下のように考える事が出来ます。

    xはx0
    yはy0
    xはy0 -> z0
    zはz0を一般化して、z'になる
    z’ は具体化してz'0であり、
    z'0はy0 -> r0
    x0 -> y0 -> r0で、x0はy0 -> z0でもあるので
    (y0 -> z0) -> y0 -> r0でおわり

z0が一般化されてしまっては困りますね。

ここで、スコープのレベルという概念を持って、スコープレベルが上がったときには、レベルの低い物は一般化しないようにします。型変数にレベルを持たせて、一般化する時はレベルが低い物は一般化しないようにします。

レベルを意識するアルゴリズムでは次のようにします。

    xはx0
    yはy0
    xはy0 -> z0 このzのレベルはxのレベルにinfer時のCallがmatch_funを呼び出したときに未束縛の型変数のレベルを使い0する。
    zはz0をレベル0で一般化しても、z0のままである。ここがポイントだ。レベルを意識しなければ一般化されるところである。
    z0 は具体化しても一般化されてないのでz0であり、
    z0はy0 -> r0

    x0 -> y0 -> r0で、x0はy0 -> z0でもあるので

    (y0 -> z0) -> y0 -> r0で
    z0はy0 -> r0だから
    (y0 -> y0 -> r0) -> y0 -> r0

一般化するときに、レベルが低い時は一般化しないのがポイントです。

コレで万事をオッケーかというと抜けがあります。unifyするときに、参照している型変数のレベルが現在レベルより高い時があるとしたら、それは一度レベルの高いところで参照されていた物でしょう。だけど、もう、今のレベルから見られているので下げる必要があります。だから、出現チェック時にはレベルを下げます。


もう一度レベルに注目してソースを読んでみましょう。

new\_var は新しい未束縛の型変数をつくりますがそのときにlevelを保持します。

      def new_var(level:level):Ty = TVar(Unbound(next_id(), level))

occurs\_check\_adjust\_levelsは再帰的な型の出現チェックを行い、無限ループを防ぎます。

      def occurs_check_adjust_levels(tvar_id:id, tvar_level:level, ty:Ty) {
        def f(ty:Ty) {
          ty match {
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(_)) => assert(false)
            case other_tvar @ TVar(Unbound(other_id, other_level)) =>
              if (other_id == tvar_id) error("recursive types")          

ここでは、tvar_levelを受け取って型のレベルと比較します。

              if (other_level > tvar_level)
                other_tvar.a = Unbound(other_id, tvar_level)

型のレベルが渡されたレベルより大きければ、手前のスコープがもう終わってたので、渡されたレベルに下げます。

            case TApp(ty, ty_arg_list) =>
              f(ty)
              ty_arg_list.foreach(f)
            case TArrow(param_ty_list, return_ty) =>
              param_ty_list.foreach(f)
              f(return_ty)
            case TConst(_) =>
          }
        }
        f(ty)
      }

unifyは単一化関数で

      def unify(ty1:Ty, ty2:Ty) {
        if (ty1 == ty2) return
        (ty1, ty2) match {
          case (TConst(name1), TConst(name2)) if(name1 == name2) =>
          case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
              ty_arg_list1.zip(ty_arg_list2).foreach{
                case (a,b) => unify(a, b)
              }
          case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
              param_ty_list1.zip(param_ty_list2).foreach{
                case(a,b) => unify(a, b)
              }
              unify(return_ty1, return_ty2)
          case (TVar(Link(ty1)), ty2) => unify(ty1, ty2)
          case (ty1, TVar(Link(ty2))) => unify(ty1, ty2)
          case (TVar(Unbound(id1, _)), TVar(Unbound(id2, _))) if id1 == id2 =>
              assert(false) // There is only a single instance of a particular type variable.

未束縛なunboundの中にレベルが含まれているので、occurs\_check\_adjust\_levelsに渡します。

          case (tvar @ TVar(Unbound(id, level)), ty) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)
これもそうです。

          case (ty, tvar @ TVar(Unbound(id, level))) =>
              occurs_check_adjust_levels(id, level, ty)
              tvar.a = Link(ty)

後は終わりです。

          case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
        }
      }

generalizeは一般化する関数でレベルを受け取ります。

      def generalize(level:level, ty:Ty):Ty = {
        ty match {

未束縛な型変数があったら、レベルが高い時だけ、一般化します。レベルが低い時は一般化しないことで単一化されなくなる事を防ぎます。ここがポイントです。

          case TVar(Unbound(id, other_level)) if other_level > level =>
            TVar(Generic(id))

他のTAppやTArrow,TVarのリンクは再帰的にレベルを引き渡します。

          case TApp(ty, ty_arg_list) =>
            TApp(generalize(level, ty), ty_arg_list.map(generalize(level, _)))
          case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(generalize(level, _)), generalize(level, return_ty))
          case TVar(Link(ty)) => generalize(level, ty)
          case TVar(Generic(_)) | TVar(Unbound(_, _)) | TConst(_) => ty
        }
      }

intantiateは具体化する関数でここもlevelを受け取ります。

      def instantiate(level:level, ty:Ty):Ty = {
        var id_var_map = Map[id,Ty]()
        def f (ty:Ty):Ty = {
          ty match {
            case TConst(_) => ty
            case TVar(Link(ty)) => f(ty)
            case TVar(Generic(id)) =>
              id_var_map.get(id) match {
                case Some(a) => a
                case None =>

型変数を作るときにlevelを保存するだけです。

                  val var1 = new_var(level)
                  id_var_map = id_var_map + (id -> var1)
                  var1
              }
            case TVar(Unbound(_,_)) => ty
            case TApp(ty, ty_arg_list) =>
              TApp(f(ty), ty_arg_list.map(f))
            case TArrow(param_ty_list, return_ty) =>
              TArrow(param_ty_list.map(f), f(return_ty))
          }
        }
        f(ty)
      }

match\_fun\_tyは関数の型のマッチングを行うinfer関数の補助関数です。

      def match_fun_ty(num_params: Int, ty: Ty): (List[Ty], Ty) = {
        ty match {
          case TArrow(param_ty_list, return_ty) =>
            if (param_ty_list.length != num_params)
              error("unexpected number of arguments")
            (param_ty_list, return_ty)
          case TVar(Link(ty)) => match_fun_ty(num_params, ty)

未束縛な型変数の時は、レベルを取り出して、引数とリターン値用の新しい型変数をレベル付きでつくります。

          case tvar@TVar(Unbound(id, level)) =>
            val param_ty_list = List.fill(num_params){new_var(level)}
            val return_ty = new_var(level)
            tvar.a = Link(TArrow(param_ty_list, return_ty))
            (param_ty_list, return_ty)
          case _ => error("expected a function")
        }
      }

型推論のメインのinferもlevelを受け取ります。

      def infer(env:Map[String,Ty], level:level, expr:Expr):Ty = {
        expr match {
          case Var(name) =>
            try {

具体化するときにlevelを渡し

              instantiate(level, env(name))
            } catch {
              case _:Throwable => error("variable " + name + " not found")
            }
          case Fun(param_list, body_expr) =>

型引数用にレベル付きで型変数を作り

            val param_ty_list = param_list.map{ _ => new_var(level)}
            val fn_env =
              param_list.zip(param_ty_list).foldLeft(env) {
                case(env, (param_name, param_ty)) => env + (param_name -> param_ty)
              }

型推論呼び出す時もレベル付きです。

            val return_ty = infer(fn_env, level, body_expr)
            TArrow(param_ty_list, return_ty)
          case Let(var_name, value_expr, body_expr) =>

Letのときは型推論呼び出す時にレベルを上げて呼び出します。

            val var_ty = infer(env, level + 1, value_expr)

一般化するとき

            val generalized_ty = generalize(level, var_ty)

型推論呼び出すとき

            infer (env + (var_name -> generalized_ty), level, body_expr)
          case Call(fn_expr, arg_list) =>

型推論呼び出すとき

            val (param_ty_list, return_ty) =
              match_fun_ty(arg_list.length, infer(env, level, fn_expr))
            param_ty_list.zip(arg_list).foreach{

型推論呼び出すときもレベル付きで呼び出します。

              case (param_ty, arg_expr) => unify (param_ty, infer(env, level, arg_expr))
            } 
            return_ty
        }
      }
    }

もう一度要点をまとめてみましょう。

new_varで新しい型変数を作る時はlevelを保存します。

新しい型変数は、関数のinferとmatch\_fun\_ty、instantiateでのみ作ります。

inferはレベルを受け取り、呼び出すinfer,intantiate,generalize関数にレベルを引き渡し、スコープを作るletの最初の式のときはレベルを１つあげます。関数の引数の型変数にレベルを保存します。

match\_fun\_tyの関数呼び出しの未束縛な型変数に含まれているレベルは取り出して型変数を作る時に使います。

intantiateでは一般化されている変数を具現化するときに型変数にlevelを保存します。

generalizeは未束縛な型変数があったら、型変数のレベルが現在のスコープレベルより高い時だけ一般化します。

unifyは未束縛な変数中のレベルを取り出して、occurs\_check\_adjust\_levelsに渡し、
occurs\_check\_adjust\_levelsでは、未束縛な型変数が現れたらレベルをunifyから受け取った物に下げます。

## 発展について

Olegの解説ではさらに、最適な処理を遅延化してまとめるアルゴリズムがある訳ですが、そこまではこの例では扱っていません。

この先の最適化はインスタンス化中に単相型のトラバースを避けて速くします。
また、現れる型変数の最大型レベルでの型レベルのすべての型をマークするみたいです。ちょっとここはよくわからないのですけど。
また、再帰的な出現のチェックはまとめて遅延すると速くなるということみたいです。

ここの文章はまだ翻訳しきれてないので、いい加減になってしまってます。

## まとめ

この解説では、もう2回程ソースを見直しました。最初に概要を見て、次にレベルに注目して見ました。

この文書で使われているアルゴリズムはOlegの解説のプログラムを多少変えて、enter,leaveを呼ばずに、levelを引き回したり、型変数の定義を変えてシンプルにしたものです。
さらにScalaに移植することでMLの文法になれていない人でも読みやすくなったかと思います。
括弧がある分行数は増えましたが、Scalaの集合やMapのライブラリは分かりやすいのでEnvモジュールを消しました。
また、参照は書き換え可能な変数に書き換えました。
日本語に翻訳することで日本人に取って分かりやすくし、直訳的な文章では分かり辛いかもしれないのでもう一度、
その解説を書いているのがこの解説です。

多相的な型推論の基本的な高速なアルゴリズムの実装があり、解説があるので参考にしてもらえば幸いです。

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
