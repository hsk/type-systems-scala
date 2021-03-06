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

`generalize`(一般化)関数は、レベルと型を取り、一般化された（多相的）型変数の入力レベルよりも高い(深い)レベルの型内ですべての型変数を一般化します。

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
