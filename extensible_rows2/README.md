> <sup><sub>
Extensible records with scoped labels - improved

# スコープドラベル付き拡張レコード - 改

> <sup><sub>
This is an improved implementation of type inference for [extensible records][original].

これは、[拡張可能なレコード][original]型推論を改善した実装です。

> <sup><sub>
Essentialy, instead of representing records and row types with a list of `RecordExtend`/`TRowExtend`
nodes, each of which contains a single label and an expression or a type, respectively, each node
contains many labels and a list of expressions/types for each of them. During type inference and when
a record is converted to a string, all labels are gathered together and sorted, making the records
easily comparable and canonically representable.

Essentialyは、代わりに単一のラベルおよび式またはタイプを含むそれぞれがRecordExtend/ TRowExtendノードのリストをレコードおよび行のタイプを表すの、それぞれ、各ノードは、それらの各々のための多くのラベルと式のリスト/型が含まれています。
型推論中のレコードが文字列に変換されると、すべてのラベルは、まとめて集約され、ソートされ、レコードは容易に比較可能で正準表現なっています。

> <sup><sub>
An overview of changes can be seen in this [diff][diff].

変更の概要は、この[差分][diff]で見ることができます。

[original]: https://github.com/tomprimozic/type-systems/tree/master/extensible_rows
[diff]: https://github.com/tomprimozic/type-systems/compare/f199446...f39ce0b
