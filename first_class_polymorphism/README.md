TODO:

- [ ] テストを終わらせる
- [x] 適当に翻訳する
- [ ] 翻訳をよりよく修正する

First-class polymorphism
========================

<sup><sub>
ordinary 普通の
intuitive 直感的な
manner やり方、方法
impredicative 非可述的
</sub></sup>

> <sup><sub>
This in {*an extension of ordinary Damas-Hindley-Milner type inference* that supports *first-class (impredicative) and higher-rank polymorphism* in *a very simple and intuitive manner*}, requiring *only minimal type annotations*.

これは*とても単純で直感的な方法*で*ファーストクラス(非可述的)と高階の多相型*をサポートした*普通のDamas-Hindley-Milner型推論の拡張*で、*最小限の型注釈だけ*を必要としています。

> <sup><sub>
> Overview

概要
-----

<sup><sub>
although だけれども、しかし、であるが
quite 非常に
limited 限られた
neither 否定に続いて以下の文でもない
above 上記、上に
undecidable 否定出来ない、申し分ない
complicated 複雑な
capable 能力のある, 可能な, 有能な
</sub></sup>

> <sup><sub>
Although *classical Damas-Hindley-Milner type inference* supports *polymorphic types*, *polymorphism* is *quite limited*: *function parameters* cannot have *polymorphic types* and neither can *type variables* be instantiated *with them*.
*On the other hand* *we* have [System F][system_f], which supports *all of the above*, but for which *type inference* is *undecidable*.
In 2003, *Didier Le Botlan and Didier Rémy* presented [ML<sup>F</sup>][mlf], *a system capable of inferring polymorphic types with minimal type annotations* (*only function parameters used polymorphically* need to be annotated), which however uses *complicated bounded types* and even more *complicated type inference algorithm*.

*古典的な Damas-Hindley-Milner の型推論*は*多相的な型*をサポートしていますが、*多相性*は*非常に限られています*：*関数のパラメータ*は、*多相的な型*を持つことができず、どちらも*型変数*に*それら*を具体化することもできません。
*一方*、*上記のすべて*がサポートされている[System F][system_f]はありますが、*型推論*は*決定不能*です。
2003年に*Didier Le BotlanとDidier Rémy*によって発表された[ML<sup>F</sup>][mlf]は*最小限の型注釈を持つ多相型を推論することができるシステム*(*多相性を使った関数パラメータのみ*注釈されていることが必要)ですが、*複雑な有界型*とより*複雑な型推論アルゴリズム*も使用しています。

<sup><sub>
contrast コントラスト、対比
intuitive 直感的な
considerably かなり
since なぜなら〜だからだ
least 最小
proposed 提案された
n-ary n項の
rigid 固い
significantly かなり，著しく，はっきりと
increase 増加，増大，増進，増殖
expressive 表現的な
improve 進歩させる
practical 実用的な
</sub></sup>

> <sup><sub>
*This implementation* is based on *the work of Daan Leijen*, published *in his paper* [HMF: Simple Type Inference for First-Class Polymorphism][hmf].
*In contrast* to ML<sup>F</sup>, *it* only uses *very intuitive System F types* and has *a considerably simpler type inference algorithm*, yet it requires *more type annotations*, since it always infers *the least polymorphic type*.
In addition to *the basic algorithm*, *this implementation* includes *some extensions* proposed by *Daan in his paper*, including *rigid annotations* and support for *n-ary function calls*, which can significantly increase *HMF's expressive power* and improve *its practical usability*.

*この実装*は *Daan Leijen の研究*に基づいており、*彼の論文*[HMF: ファーストクラス多相型のためのシンプルな型推論][hmf]で発表されました。
ML<sup>F</sup>とは*対照的に*、*これ*は*非常に直感的なSystem Fの型*のみを使用しており、*かなり単純な型推論アルゴリズム*を持ち、*最小の多相型*を常に推論するので、*より多くの型注釈*が必要です。
*基本的なアルゴリズム*に加えて、*この実装*は、*彼の論文でDaan*により提案された*いくつかの拡張*を含んでおり、*n項の関数呼び出し*のための*固い注釈*のサポートなど、*かなりHMFの表現力*と*実用的な使いやすさ*を向上させることができます。

> <sup><sub>
*Features* supported by *HMF*:

*HMF*によってサポートされている*機能*:

  - *polymorphic type parameters* 多相的型引数

    <sup><sub>
    indeed たしかに、まあ，さよう、いかにも
    </sub></sup>

    > <sup><sub>
    *Parameters used polymorphically* require *type annotations* even in *ML<sup>F</sup>*; indeed, without *the type annotation* this would more likely be *programmer error*.

    *多相的に使用されるパラメータ*は、ML<sup>F</sup>でも*型注釈*が必要です;たしかに、*型注釈*なしでは、より多くの*プログラマエラー*になりそうです。

    ```
    let poly = fun (f : forall[a] a -> a) -> pair(f(one), f(true)) in
    poly : (forall[a] a -> a) -> pair[int, bool]
    ```

    > <sup><sub>
    *Parameters* not used *polymorphically*, but only passed on to *other functions as polymorphic arguments*, need to be *annotated (unlike in ML<sup>F</sup>)*.

    *パラメータ*は、*多相的*に使用されていませんが、唯一の*多相引数として他の関数*に渡されるさいに、*(ML<sup>F</sup>と異なり）注釈*が必要です。

    ```
    let f = fun (x : forall[a] a -> a) -> poly(x) in
    f : (forall[a] a -> a) -> pair[int, bool]
    ```

  - *impredicative polymorphism* 非可述的多相性

    <sup><sub>
    During 間に、中に
    below 以下、次
    correct 正しい
    </sub></sup>

    > <sup><sub>
    *Type variables such as `a` and `b` in `(a -> b, a) -> b`* can be instantiated to *polymorphic type*.
    During *type checking of the example below*, *the type variable `a`* is instantiated to *a polymorphic type `forall[c] c -> c`*, and *the correct type* is inferred.

    *`(a -> b, a) -> b` の `a` と `b` のような型変数*は*多相的な型*に具体化することができます。
    *以下の例の型チェック*時に、*型変数`a`*は、*多相型`forall[c] c -> c`*に具体化され、*正しい型*が推論されます。

    ```
    let apply = fun f x -> f(x) in
    apply : forall[a b] (a -> b, a) -> b

    apply(poly, id) : pair[int, bool]
    ```

  - *rigid type annotations* 厳格な型注釈

    <sup><sub>
    absence 欠席
    least 最低、最小
    specify 特定する
    </sub></sup>

    > <sup><sub>
    In *absence of type annotations*, *HMF* will *always* infer *the least polymorphic type*.

    *型注釈がない*場合に、*HMF*は*常に* *最小の多相的な型*を推論するでしょう。

    ```
    let single = fun x -> cons(x, nil) in
    single : forall[a] a -> list[a]

    let ids = single(id) in
    ids : forall[a] list[a -> a]
    ```

    > <sup><sub>
    *The programmer* can specify *a more polymorphic type* for `ids` using *type annotations*.

    *プログラマ*は、*型注釈*を使用して`ids`のために*さらなる多相的な型*を指定できます。

    ```
    let ids = single(id : forall[a] a -> a) in
    ids : list[forall[a] a -> a]
    ```

> <sup><sub>
> Details

詳細
-------

<sup><sub>
represents 表す
quantifiers 数量子
unspecified 不特定、未定義
</sub></sup>

> <sup><sub>
*Types and expressions of HMF* are *simple extensions* of *what we had in `algorithm_w`*.
*We* add *the `TForall` constructor*, which enables us *to construct polymorphic types*, and now have *3 types of type variables*: *`Unbound` type variables* can be unified with *any other type*, `Bound` represents *those variables* bound by *`forall` quantifiers or type annotations*, and *`Generic` type variables* help us typecheck *polymorphic parameters* and can only be unified with *themselves*. 

*HMFの型と式*は、*我々が `algorithm_w` で用いていた物*の*単純な拡張*です。
*我々*は、*多相的な型を構築*を可能にする*`TForall`コンストラクタ*を追加し、

    case class TForall(a:List[id], b:ty) extends ty          // polymorphic type: `forall[a] a -> a`

現在、*型変数の3つの型*を持ちます:
*`Unbound`型変数*は、*他の型*と単一化することができ、
`Bound`は*`forall`量指定子または型注釈*によって束縛された*これらの変数*を表し、
*`Generic`型変数*は、*多相的なパラメータ*の型チェックで私たちを助け、*自分自身*だけで単一化することができます。

    sealed trait tval
    case class Unbound(a: id, b: level) extends tval
    case class Link(a: ty) extends tval
    case class Generic(a: id) extends tval
    case class Bound(a: id) extends tval

*Expressions* are extended with *type annotations `expr : type`*.
*Type annotations* can bind *additional unspecified type variables*, and can also appear on *function parameters*:

*式*は *型注釈* `expr : type` で拡張されています。
*型注釈*は、*追加の未確定の型変数*を束縛でき、また、*関数のパラメータ*に書く事が出来ます:

```
let f_one = fun f -> f(one) in
f_one : forall[a] (int -> a) -> a

let f_one_ann = fun (f : some[a] a -> a) -> f(one) in
f_one_ann : (int -> int) -> int
```

<sup><sub>
important 重要
canonical 標準的な
efficiently 効果的に
</sub></sup>

> <sup><sub>
*An important part of the type inference* is *the `replace_ty_constants_with_vars` function in `parser.scala`*, which turns *type constants `a` and `b` into `Bound` type variables* if *they* are *bound by `forall[a]` or `some[b]`*.

*型推論の重要な部分*は*`parser.scala`内の`replace_ty_constants_with_vars`関数*で、*それら*が*`forall[a]`または`some[b]`によって束縛されている*場合に*型定数`a`と`Bound`型変数内の`b`に*変えます。

> <sup><sub>
*This function* also normalizes *the type*, ordering *the bound variables* in order in which *they* appear in *the structure of the type*, and removing *unused ones*.

*この関数*は、*型を*正規化し、*型の構造*に表示されている順に*バインドされた変数*を順序付け、*未使用のもの*を除去します。

> <sup><sub>
*This* turns *different versions of the same type `forall[b a] a -> b` and `forall[c a b] a -> b`* into *a canonical representation `forall[a b] a -> b`*, allowing *the type inference engine* to efficiently unify *polymorphic types*.


*これ*は、*同じ型の`forall[b a] a -> b`と`forall[c a b] a -> b`の異なるバージョン*の*標準的な表現`forall[a b] a -> b`*に変わり、*型推論エンジン*が*多相型*を効率的に単一化することができます。

    def replace_ty_constants_with_vars (var_name_list:List[String], ty: ty):(List[id], ty) = {
      var name_map = Map[String,Option[ty]]()
      val var_id_list_rev_ref = Ref(List[id]())

      var_name_list.foreach {
        (var_name) => name_map = name_map + (var_name -> None)
      }  
      
      def f(ty:ty):ty = {
        ty match {
        case TConst(name) =>
          try {
            name_map(name) match {
              case Some(v) => v
              case None =>
                val (var_id, v) = new_bound_var()
                var_id_list_rev_ref.a = var_id :: var_id_list_rev_ref.a
                name_map = name_map + (name -> Some(v))
                v
            }
          } catch {
            case _:Throwable => ty
          }
        case TVar(_) => ty
        case TApp(ty, ty_arg_list) =>
          val new_ty = f(ty)
          val new_ty_arg_list = ty_arg_list.map(f) 
          TApp(new_ty, new_ty_arg_list)
        case TArrow(param_ty_list, return_ty) =>
          val new_param_ty_list = param_ty_list.map(f)
          val new_return_ty = f(return_ty)
          TArrow(new_param_ty_list, new_return_ty)
        case TForall(var_id_list, ty) => TForall(var_id_list, f(ty))
        }
      }
      val ty2 = f(ty)
      (var_id_list_rev_ref.a.reverse, ty2)
    }


<sup><sub>
respective それぞれの
appear 現れる、表示する
determined 決定
</sub></sup>

> <sup><sub>
*Function `substitute_bound_vars` in `infer.scala`* takes *a list of `Bound` variable ids, *a list of replacement types and a type* and returns *a new type with `Bound` variables* substituted with *respective replacement types*.

*`infer.scala`内の関数`substitute_bound_vars`*は *`Bound`変数idsのリスト、置換型リストと型*をとり、*それぞれの置き換えた型*で代入した*`Bound`変数である新しい型*を返します。

    def substitute_bound_vars(var_id_list:List[id], ty_list:List[ty], ty:ty):ty = {
      def f(id_ty_map:Map[id,ty], ty:ty):ty = {
        ty match {
        case TConst(_) => ty
        case TVar(Ref(Link(ty))) => f(id_ty_map, ty)
        case TVar(Ref(Bound(id))) => id_ty_map.getOrElse(id, ty)
            
        case TVar(_) => ty
        case TApp(ty, ty_arg_list) =>
            TApp(f(id_ty_map, ty), ty_arg_list.map(f(id_ty_map, _)))
        case TArrow(param_ty_list, return_ty) =>
            TArrow(param_ty_list.map(f(id_ty_map, _)), f(id_ty_map, return_ty))
        case TForall(var_id_list, ty) =>
            TForall(var_id_list, f(int_map_remove_all(id_ty_map, var_id_list), ty))
        }
      }
      f(int_map_from_2_lists(var_id_list, ty_list), ty)
    }

> <sup><sub>
*Function `escape_check`* takes a list of *`Generic` type variables* and *types `ty1` and `ty2`* and checks if any of *the `Generic` type variables* appears in any of *the sets of free generic variables in `ty1` or `ty2`*, which are determined *using the function `free_generic_vars`*.

*`escape_check`関数*は*`Generic`型変数群*のリストと*型`ty1`と`ty2`*を取り、
*`Generic`型変数*内のどれかが*`ty1`か`ty2`内の自由な一般的な変数*のどれかの(これは、*関数 `free_generic_vars`を使用して*決定されます)中にあるかをチェックします。

    def escape_check(generic_var_list:List[ty], ty1:ty, ty2:ty):Boolean = {
      val free_var_set1 = free_generic_vars(ty1)
      val free_var_set2 = free_generic_vars(ty2)
      generic_var_list.exists{
        generic_var =>
          free_var_set1.contains(generic_var) || free_var_set2.contains(generic_var)
      }      
    }

    def free_generic_vars(ty:ty):Set[ty] = {
      var free_var_set = Set[ty]()
      def f(ty:ty) {
        ty match {
        case TConst(_) =>
        case TVar(Ref(Link(ty))) => f(ty)
        case TVar(Ref(Bound(_))) =>
        case TVar(Ref(Generic(_))) =>
            free_var_set += ty
        case TVar(Ref(Unbound(_,_))) =>
        case TApp(ty, ty_arg_list) =>
            f(ty)
            ty_arg_list.foreach(f)
        case TArrow(param_ty_list, return_ty) =>
            param_ty_list.foreach(f)
            f(return_ty)
        case TForall(_, ty) => f(ty)
        }
      }
      f(ty)
      free_var_set
    }

<sup><sub>
rely 頼る
equivalent 等価、同等の
substitute 代入
otherwise 一方
</sub></sup>

> <sup><sub>
*The main difference in function `unify`* is *the unification of polymorphic types*.

*関数 `unify`の主な違い*は*多相的な型の単一化*です。

    def unify(ty1:ty, ty2:ty) {
      println("unify "+ty1+" "+ty2)
      if (ty1 == ty2) return
      (ty1, ty2) match {
        case (TConst(name1), TConst(name2)) if (name1 == name2) => ()
        case (TApp(ty1, ty_arg_list1), TApp(ty2, ty_arg_list2)) =>
            unify(ty1, ty2)
            iter2(ty_arg_list1, ty_arg_list2, unify)
        case (TArrow(param_ty_list1, return_ty1), TArrow(param_ty_list2, return_ty2)) =>
            iter2(param_ty_list1, param_ty_list2, unify)
            unify(return_ty1, return_ty2)
        case (TVar(Ref(Link(ty1))), ty2) => unify(ty1, ty2)
        case (ty1, TVar(Ref(Link(ty2)))) => unify(ty1, ty2)
        case (TVar(Ref(Unbound(id1, _))), TVar(Ref(Unbound(id2, _)))) => assert(false)
        case (TVar(Ref(Generic(id1))), TVar(Ref(Generic(id2)))) if (id1 == id2) =>
            /* This should be handled by the `ty1 == ty2` case, as there should
               be only a single instance of a particular variable. */
            assert(false)
        case (TVar(Ref(Bound(_))), _) | (_, TVar(Ref(Bound(_)))) =>
            // Bound vars should have been instantiated.
            assert(false)
        case (TVar(tvar@Ref(Unbound(id, level))), ty) =>
            occurs_check_adjust_levels(id, level, ty)
            tvar.a = Link(ty)
        case (ty, TVar(tvar@Ref(Unbound(id, level)))) =>
            occurs_check_adjust_levels(id, level, ty)
            tvar.a = Link(ty)

        case (forall_ty1@TForall(var_id_list1, ty1), forall_ty2@TForall(var_id_list2, ty2)) =>

> <sup><sub>
First, *we* create *a fresh `Generic` type variable* for *every type variable* bound by *the two polymorphic types*.

まず、*我々*は*2つの多相型*によって束縛された*すべての型変数*のための*新鮮な`Generic`型変数*を作成します。

> <sup><sub>
Here, *we* rely on *the fact* that *both types* are *normalized*, so equivalent *generic type variables* should appear in *the same locations in both types*.

ここで、*我々*は*両方の型*が*正規化されている*という*事実*の上に依存し、同等の*ジェネリック型変数*は、*両方の型で同じ位置*に現われる必要があります。

            val l1 = var_id_list1.length
            val l2 = var_id_list2.length
            if(l1 != l2)
                error ("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))

            val generic_var_list = (for(i <- 0 until l1) yield { new_gen_var() }).toList

> <sup><sub>
Then, *we* substitute *all `Bound` type variables* in *both types* with *`Generic` type variables*, and try to unify *them*.

その後、*我々*は*`Generic`型変数*と*両方の型*の中の*すべての`Bound`型変数*を代入し、*それら*の単一化を試みます。

            val generic_ty1 = substitute_bound_vars(var_id_list1, generic_var_list, ty1)
            val generic_ty2 = substitute_bound_vars(var_id_list2, generic_var_list, ty2)
            unify(generic_ty1, generic_ty2)

> <sup><sub>
If *unification* succeeds, *we* check that *none of the `Generic` type variables "escapes"*, otherwise *we* would successfully unify *types `forall[a] a -> a` and `forall[a] a -> b`*, where `b` is *a unifiable `Unbound` type variable*.

*単一化*に成功した場合、*我々* は *`Generic`型変数のいずれも「エスケープしない」ことを*確認し、そうでない場合、*我々*は`b`が*単一化可能な`Unbound`型変数*である*型`forall[a] a -> a` と `forall[a] a -> b`*を正常に単一化します。

            if (escape_check(generic_var_list, forall_ty1, forall_ty2))
              error ("cannot unify types " + string_of_ty(forall_ty1) + " and " + string_of_ty(forall_ty2))
        case (_, _) => error("cannot unify types " + string_of_ty(ty1) + " and " + string_of_ty(ty2))
      }
    }

    def escape_check(generic_var_list:List[ty], ty1:ty, ty2:ty):Boolean = {
      val free_var_set1 = free_generic_vars(ty1)
      val free_var_set2 = free_generic_vars(ty2)
      generic_var_list.exists{
        generic_var =>
          free_var_set1.contains(generic_var) || free_var_set2.contains(generic_var)
      }      
    }

> <sup><sub>
*Function `instantiate`* instantiates *a `forall` type* by *substituting bound type variables* by *fresh `Unbound` type variables*, which can then by *unified with any other type*.

*他のタイプで単一化*することができる、*新鮮な `Unbound` 型変数*によって*結合された型変数を代入*して、*関数 `instantiate`* は *`forall` 型*を具体化します。

    def instantiate(level:level, ty:ty):ty = {
      ty match {
      case TForall(var_id_list, ty) =>
          val (var_list, instantiated_ty) = substitute_with_new_vars(level, var_id_list, ty)
          instantiated_ty
      case TVar(Ref(Link(ty))) => instantiate(level, ty)
      case ty => ty
      }
    }

> <sup><sub>
*Function `instantiate_ty_ann`* does the same for *type annotations*.

*関数 `instantiate_ty_ann`* は、*型注釈*のために同じことを行います。

    def instantiate_ty_ann(level:level,v:(List[id], ty)):(List[ty], ty) = {
      v match {
      case (List(), ty) => (List(), ty)
      case (var_id_list, ty) => substitute_with_new_vars(level, var_id_list, ty)
      }
    }

> <sup><sub>
*The function `generalize`* transforms *a type* into *a `forall` type* by *substituting all `Unbound` type variables* with *levels* higher than *the input level* with *`Bound` type variables*.

*関数 `generalize`* は、*`Bound` 型変数*を使って*入力レベル*よりも高い*レベル*で*すべての `Unbound`型変数を代入すること*により、 *`forall` 型*に*型*変換します。

    def generalize(level:level, ty:ty):ty = {
      val var_id_list_rev_ref = Ref(List[id]())
      def f(ty:ty) {
        ty match {
        case TVar(Ref(Link(ty))) => f(ty)
        case TVar(Ref(Generic(_))) => assert(false)
        case TVar(Ref(Bound(_))) =>
        case TVar(other_tvar@Ref(Unbound(other_id, other_level))) if (other_level > level) =>
            other_tvar.a = Bound(other_id)
            if (!var_id_list_rev_ref.a.contains(other_id)) {
              var_id_list_rev_ref.a = other_id :: var_id_list_rev_ref.a
            }
        case TVar(Ref(Unbound(_,_))) =>
        case TApp(ty, ty_arg_list) =>
            f(ty)
            ty_arg_list.foreach(f)
        case TArrow(param_ty_list, return_ty) =>
            param_ty_list.foreach(f)
            f(return_ty)
        case TForall(_, ty) => f(ty)
        case TConst(_) =>
        }
      }
      f(ty)
      var_id_list_rev_ref.a match {
        case List() => ty
        case var_id_list_rev => TForall(var_id_list_rev.reverse, ty)
      }
    }

> <sup><sub>
*It* traverses *the structure of the types* in *a depth-first, left-to-right order*, same as *the function `replace_ty_constants_with_vars`*, making sure that *the resulting type* is *in normal form*.

これは、*深さ優先で左から右の順に移動する*型の構造を、*`replace_ty_constants_with_vars` 関数*と同じように、変換し、*結果の型*は*正規形*であることを確認します。

<sup><sub>
subsume 包含する
</sub></sup>

> <sup><sub>
*The function `subsume`* takes *two types `ty1` and `ty2`* and determines if `ty1` is *an instance of `ty2`*.

*関数 `subsume`* は *2つの型 `ty1` と `ty2`* を取り、`ty1` が *`ty2` のインスタンス*であるかどうかを判別します。

> <sup><sub>
For example, `int -> int` is *an instance of `forall[a] a -> a` (the type of `id`)*, which in turn is *an instance of `forall[a b] a -> b` (the type of `magic`)*.

例えば、`int -> int` は `forall[a] a -> a` (`id`の型)のインスタンスで、ひいては `forall[a b] a -> b` (`magic`の型)のインスタンスです。

> <sup><sub>
*This* means that *we* can pass `id` as *an argument* to *a function expecting `int -> int`* and *we* can pass `magic` to *a function expecting `forall[a] a -> a`*, but *not the other way* round.

*これ*は、*我々*が関数に予期される `int -> int` への*引数*として `id` を渡すことができ、`forall[a] a -> a` 予期関数に順番を逆にしないで `magic` を渡すことができることを意味します。

    def subsume(level:level, ty1:ty, ty2:ty) {

> <sup><sub>
To determine if `ty1` is *an instance of `ty2`*, `subsume` first instantiates `ty2`, *the more general type*, with *`Unbound` type variables*.

`ty1`が*`ty2`のインスタンス*であるかどうかを判断するには、`subsume`は最初*`Unbound`型変数*で*一般的な型*の`ty2`を具体化します。

      val instantiated_ty2 = instantiate(level, ty2)
      unlink(ty1) match {

> <sup><sub>
If `ty1` is not *polymorphic*, is simply unifies *the two types*.

`ty1`が*多相型*でない場合は、*2つの型*を単に単一化します。

> <sup><sub>
Otherwise, *it* instantiates *`ty1` with `Generic` type variables* and unifies *both instantiated types*.

それ以外の場合は、*`Generic`型変数で`ty1`*を具体化し、そして*両方の具体化された型*を単一化します。

> <sup><sub>
If *unification* succeeds, *we* check that no *generic variable* escapes, same as in `unify`.

*単一化*に成功した場合、`unify`と同じで、*一般的な変数*がエスケープしないことを確認します。

        case forall_ty1@TForall(var_id_list1, ty1) =>
            val generic_var_list = var_id_list1.map( _ => new_gen_var())
            val generic_ty1 = substitute_bound_vars(var_id_list1, generic_var_list, ty1)
            unify(generic_ty1, instantiated_ty2)
            if (escape_check(generic_var_list, forall_ty1, ty2))
              error ("type " + string_of_ty(ty2) + " is not an instance of " + string_of_ty(forall_ty1))
        case ty1 => unify(ty1, instantiated_ty2)
      }
    }

<sup><sub>
significantly 大幅に
instead それよりも、かえって
might be かもしれません
</sub></sup>

> <sup><sub>
*Type inference in function `infer`* changed significantly.

*`infer`関数内の型推論*は大幅に変更されました。

    def infer(env:Env.env, level:level, expr:expr):ty = {
      println("infer "+expr)
      expr match {

> <sup><sub>
*We* no longer instantiate *the polymorphic types of variables and generalize types at let bindings*, but instantiate at *function calls* and generalize at *function calls and function definitions* instead.

*私たち*はもはや、変数の多相型を具体化とletバインディングで型を一般化しませんが、その代わりに*関数呼び出し*で具体化し、*関数呼び出しと関数定義*で一般化します。

      case Var(name) =>
          try {
            Env.lookup(env, name)
          } catch {
            case _:Throwable => error ("variable " + name + " not found")
          }
      case Fun(param_list, body_expr) =>

> <sup><sub>
To infer *the types of functions*, *we* first extend *the environment with the types of the parameters*, which might be *annotated*.

*関数の型*を推論するために、*我々*は*注釈された*可能性がある*パラメータの型による環境*を最初に拡張します。

          val fn_env_ref = Ref(env)
          val var_list_ref = Ref(List[ty]())

> <sup><sub>
*We* remember *all new type variables* that appear in *parameter types*, so that *we* can later make sure that none of *them* was *unified with a polymorphic type*.

*我々*は*パラメータ型*内で現れる*すべての新しい型変数*を記憶します。*それら*が*多相型で単一化された*ものでないことを*我々*が後で確認することができるように。

          val param_ty_list = param_list.map {
            case (param_name, maybe_param_ty_ann) =>
              val param_ty = maybe_param_ty_ann match {
                case None => // equivalent to `some[a] a`
                    val v = new_var(level + 1)
                    var_list_ref.a = v :: var_list_ref.a
                    v
                case Some(ty_ann) =>
                    val (var_list, ty) = instantiate_ty_ann(level + 1, ty_ann)
                    var_list_ref.a = var_list ::: var_list_ref.a
                    ty
              }
              fn_env_ref.a = Env.extend(fn_env_ref.a, param_name, param_ty)
              param_ty
            }

> <sup><sub>
*We* then infer *the type of the function body* using *the extended environment*, and instantiate *it* unless *it's annotated*.

*我々*は*拡張された環境*を使用して*関数本体の型*を推論し、そして*それが注釈付き*の場合を除き、*それ*を具体化します。
          
          val inferred_return_ty = infer(fn_env_ref.a, level + 1, body_expr)

          val return_ty =
            if (is_annotated(body_expr)) inferred_return_ty
            else instantiate (level + 1, inferred_return_ty)

> <sup><sub>
Finally, *we* generalize *the resulting function type*.

最後に、*我々*は*関数の結果の型*を一般化します。

          if (!var_list_ref.a.forall(is_monomorphic))
            error ("polymorphic parameter inferred: "
              + var_list_ref.a.map(string_of_ty).mkString(", "))
          else
            generalize(level, TArrow(param_ty_list, return_ty))

      case Let(var_name, value_expr, body_expr) =>
          val var_ty = infer(env, level + 1, value_expr)
          infer(Env.extend(env, var_name, var_ty), level, body_expr)

> <sup><sub>
To infer *the type of function application* *we* first infer *the type of the function being called*, instantiate *it* and separate *parameter types* from *function return type*.

*関数適用の型*を推論するために、*我々*は*関数が呼び出された型*をまず推論して、*それ*を具体化し、*パラメータ型*を*関数の戻り値の型*から分離します。

      case Call(fn_expr, arg_list) =>
          val fn_ty = instantiate(level + 1, infer(env, level + 1, fn_expr))
          val (param_ty_list, return_ty) = match_fun_ty(arg_list.length, fn_ty)
          infer_args(env, level + 1, param_ty_list, arg_list)
          generalize(level, instantiate(level + 1, return_ty))

> <sup><sub>
*The core of the algorithm* is *infering argument types* in *the function `infer_args`*.

*アルゴリズムのコア*は、*関数 `infer_args`* 内の*引数の型の推論*です。

          def infer_args(env:Env.env, level:level, param_ty_list:List[ty], arg_list:List[expr]) {
            
            val pair_list = param_ty_list.zip(arg_list)
            def get_ordering(ty:ty, arg:Any):Int = {
              // subsume type variables last
              unlink(ty) match {
                case TVar(Ref(Unbound(_,_))) => 1
                case _ => 0
              }
            }
            val sorted_pair_list = pair_list.sortWith{
              case ((ty1, arg1), (ty2, arg2)) =>
                get_ordering(ty1, arg1) > get_ordering(ty2, arg2)
            }
            
            sorted_pair_list.foreach {
              case (param_ty, arg_expr) =>
                val arg_ty = infer(env, level, arg_expr)

> <sup><sub>
After *infering the type of the argument*, *we* use *the function `subsume`* (or `unify` if *the argument* is *annotated*) to determine if *the parameter type* is *an instance of the type of the argument*.

*引数の型の推論*後、*我々は*、*パラメータの型*が*引数の型のインスタンス*であるかどうかを判断するために、*関数 `subsume`*（または*引数*が*注釈されている*場合は`unify`）を使用します。

                if (is_annotated(arg_expr))
                  unify(param_ty, arg_ty)
                else
                  subsume(level, param_ty, arg_ty)
            }
          }


> <sup><sub>
When *calling functions with multiple arguments*, *we* must first subsume *the types of arguments* for *those parameters* that are not *type variables*, otherwise *we* would fail to typecheck *applications such as `rev_apply(id, poly)`*, where `rev_apply : forall[a b] (a, a -> b) -> b`, `poly : (forall[a] a -> a) -> pair[int, bool]` and `id : forall[a] a -> a`.

*複数の引数で関数が呼ばれる*ときは、我々は先に*型変数*ではない*それらのパラメータ* のために*引数の型*をsubsumeする必要があり、
そうでないとき我々は`rev_apply : forall[a b] (a, a -> b) -> b`, `poly : (forall[a] a -> a) -> pair[int, bool]` and `id : forall[a] a -> a`である`rev_apply(id, poly)`のような適用の型検査を失敗させるでしょう。

> <sup><sub>
*Infering type annotation `expr : type`* is equivalent *to inferring the type of a function call `(fun (x : type) -> x)(expr)`*, but optimized in *this implementation of `infer`*.

*型注釈`expr : type`の推論*は*関数呼び出し`(fun (x : type) -> x)(expr)`の型を推論する事*と同じですが、この`infer`の実装は最適化されています。

      case Ann(expr, ty_ann) =>
          val (_, ty) = instantiate_ty_ann(level, ty_ann)
          val expr_ty = infer(env, level, expr)
          subsume(level, ty, expr_ty)
          ty
      }
    }


> <sup><sub>
> Extensions

拡張
--------

<sup><sub>
describe 説明する
desirable 望ましい
</sub></sup>

> <sup><sub>
*Daan Leijen* also published *a reference implementation ([.zip][hmf-ref]) of HMF*, *written in Haskell*.

*Daan Leijen* はまた *Haskell で書かれた* *HMF のリファレンス実装 ([.zip][hmf-ref])*を公開しています。

> <sup><sub>
In addition to *the type inference algorithm* describe in *his paper*, *he* implemented *an interesting extension* to *the algorithm* that significantly improves *the usability of HMF*.

*彼の論文*の*型推論アルゴリズム*の記述に加えて、*彼*は*かなりHMFの使い勝手*を向上させる*アルゴリズム*に*興味深い拡張機能*を実装しました。

> <sup><sub>
In **Overview** *we* saw that in order to create *a list of polymorphic functions `ids : list[forall[a] a -> a]`*, *the programmer* must add *a type annotation `let ids = single(id : forall[a] a -> a)`*, otherwise HMF infers *the least polymorphic type*.

**概要**では、*我々*は順番に*多相関数の `ids : list[forall[a] a -> a]` のリスト*を作成することを見ましたが、*プログラマ*は *`let ids = single(id : forall[a] a -> a)` 型注釈*を追加する必要があり、そうでない場合*HMF*は*最小の多相型*を推論します。

> <sup><sub>
*The type annotation* must be provided on *the argument* to *the function `single`*, in order to prevent *the argument type from being instantiated*.

*具体化された引数の型*を防止するために、*型注釈*は、*関数 `single`*の*引数*に与えられなければなりません。

> <sup><sub>
However, it would be more desirable for *the programmer* to be able to specify *the type of the result of function `single`*:

しかし、*プログラマ*が*関数 `single`の結果の型*を指定できるようにすることがより望ましいでしょう。

```
let ids = single(id) : list[forall[a] a -> a]
```

<sup><sub>
propagating 伝搬
propagate 伝搬する
indicate 示す、物語る
whether かどうか
</sub></sup>

> <sup><sub>
*We* can implement *this* in *the type inference algorithm* by *propagating the information about expected types* from *function result type* to *function arguments* and to *parameter types* of *functions expressions*.

*私たち*は、*関数の引数*と*関数式*の*パラメータの型*には、*関数結果の型*から*予想される型に関する情報を伝播させること*によって*型推論アルゴリズム*で*これ*を実装することができます。

> <sup><sub>
To *function `infer`* *we* add *two additional arguments `maybe_expected_ty`*, for *optionally specifying the resulting type*, and `generalized`, which indicates whether *the resulting type* should be *generalized or instantiated*.

*関数`infer`*のために、*我々* は `maybe_expected_ty`が*必要に応じて結果の型を指定し*、そして`generalized`が*結果の型*は*一般化または具体化*する必要があるかどうかを示す、*2つの追加引数*を加えます。

> <sup><sub>
To *infer the type of function expression*, *we* annotate *the unannotated parameters with expected parameter types* and use *the expected return type to infer the type of the function body*.

*関数式の型を推論する*ために、*我々*は、*予想されるパラメータ型に注釈のないパラメータ*に注釈し、*関数本体の型を推論することが期待戻り値の型*を使います。

> <sup><sub>
To *propagate the expected type through a function application*, *we* first unify *it with the function return type*.

*関数適用から予期される型を伝播する*ために、*我々*はまず*関数の戻り値の型とそれを*単一化します。

> <sup><sub>
Then *we* infer *the types of the arguments*, taking care to first infer *the annotated arguments* and to infer *arguments for parameters which are type variables last*.

*我々*は注意深く、まず*注釈付き引数*を推論して、*最後の型変数であるパラメータの引数*を推論した後、*引数の型*を推論します。

<sup><sub>
invariant 不変
</sub></sup>

> <sup><sub>
We cannot propagate *the expected type through a function application* if *the return type of the function* is *a type variable*.

もしも*関数の戻り値の型*が*型変数*であれば*我々*は*関数適用から予想型*を伝播することはできません。

> <sup><sub>
For example, for *function `head : forall[a] list[a] -> a`*, propagating *the result type in `head(ids) : int -> int`* would instantiate *the parameter type to `list[int -> int]`*, which is not *an instance of `list[forall[a] a -> a]`* (since *this type system* is *invariant*).

例えば、*関数`head : forall[a] list[a] -> a`*では、*`head(ids) : int -> int`での結果の型*を伝播することは*`list[forall[a] a -> a]`のインスタンス* ではない (*この型システム*は*不変*であるため)*`list[int -> int]`へのパラメータの型*を、具体化することになります。

<sup><sub>
unambiguous 明白な
</sub></sup>

> <sup><sub>
*This extension* also allows *programmers to write anonymous functions* with *polymorphic arguments* without *annotations* in cases when *the function type* is *unambiguous*:

*この拡張*は、場合によって*関数の型*が*明確*なときに*プログラマ*が*注釈なし*で*多相型の引数*の*無名関数を書くこと*を許します:

```
let special = fun (f : (forall[a] a -> a) -> (forall[a] a -> a)) -> f(id) : forall[a] a -> a in
special : ((forall[a] a -> a) -> (forall[a] a -> a)) -> forall[a] a -> a

special(fun f -> f(f))
```

[system_f]: http://en.wikipedia.org/wiki/System_F
[hmf]: http://research.microsoft.com/apps/pubs/default.aspx?id=132621
[mlf]: http://gallium.inria.fr/~remy/work/mlf/
[hmf-ref]: http://research.microsoft.com/en-us/um/people/daan/download/hmf-prototype-ref.zip

----

first class polymorphism は多相的なものを多相的なまま値として受け渡せる型システムです。
