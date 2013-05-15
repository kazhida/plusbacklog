+backlog
===========

読み方：ぷらす・ばっくろぐ

backlog.jpのAPIを利用して、課題(Issue)を追加することに特化したクライアント

入力できる情報は、

* プロジェクト
* 件名
* 詳細
* 種別
* 優先度

に、絞っている。

ソースをビルドする場合は、AdMobのSDK(GoogleAdMobAdsSdk-6.4.1.jar)を組み込んで、
文字列リソース

* publisher_id (for AdMob)
* public_key_64 (for InAppBilling)

を、適切に設定する必要があります。
