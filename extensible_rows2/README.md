> <sup><sub>
Extensible records with scoped labels - improved

# スコープドラベル付き拡張レコード - 改

<sup><sub>
improved 改善された
Essentialy 基本的に
instead 代わりに
representing 表します
row 行
nodes ノード
each 各
contains 含まれている
respectively それぞれ
During 中に
converted 変換された
gathered  集まった
together 一緒に
representable 表現
</sub></sup>


> <sup><sub>
This is an improved implementation of type inference for [extensible records][original].

これは、[拡張可能なレコード][original]型推論を改善した実装です。

> <sup><sub>
Essentialy, instead of representing records and row types with a list of `RecordExtend`/`TRowExtend`
nodes,
each of which contains a single label and an expression or a type, respectively,
each node contains many labels and a list of expressions/types for each of them.
During type inference and when a record is converted to a string, all labels are gathered together and sorted,
making the records easily comparable and canonically representable.

基本的に、レコードとrowの型の`RecordExtend`/`TRowExtend`ノードリストの表現の代わりに、
各々が単一のラベルと式あるいは型を含んだ物で、それぞれ、
各々のノードが各々のためにいくつかのラベルと式/型のリストを含んでいます。
型推論中あるいはレコードが文字列にコンバートされた時に、すべてのラベルは、まとめて集約されそしてソートされ、
レコードは簡単にコンパチブルで正確な表現にされます。

> <sup><sub>
An overview of changes can be seen in this [diff][diff].

変更の概要は、この[差分][diff]で見ることができます。

[original]: https://github.com/tomprimozic/type-systems/tree/master/extensible_rows
[diff]: https://github.com/tomprimozic/type-systems/compare/f199446...f39ce0b
