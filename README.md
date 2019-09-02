# APIGateway-Lambda(Java)-KDF-S3-QuickSight ハンズオン
ハンズオンで必要になるコマンドやパラメータ群

* 各種リソース名には "APIGWHandsOn" という名前をつけます

# 作業環境の構築（Cloud9）

1. Java8環境のインストール
```bash
sudo yum -y update
sudo yum -y install java-1.8.0-openjdk-devel
```

2. Java7からJava8環境への切り替え
```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```

3. Mavenのインストール
```bash
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
```
4. サンプルコードリポジトリのチェックアウト
```bash
git clone https://github.com/yktko/apigw-lambda-kdf-sample.git
```

5. Cloud9エディタでコードの編集、Cloud9ターミナルでAWS CLIが使えることを確認
```bash
aws s3 ls
```

# Lambda(Java)を実行する
## ビルド、デプロイパッケージの作成
1. HelloPojo.java をCloud9エディタで開き、以下の2行をコメントアウト
```java
    public ResponseClass handleRequest(RequestClass request, Context context){

        String time = OffsetDateTime.now().toString();
//        firehoseClient = AmazonKinesisFirehoseClientBuilder.standard().build();
//        putRecordIntoDeliveryStream(time + "," + request.values);
```

2. Mavenを使ってjarファイルをビルド
```bash
cd apigw-lambda-kdf-sample/
mvn package
```
3. target ディレクトリに以下のjarファイルがあることを確認
```java
ls target
lambda-java-example-1.0-SNAPSHOT.jar 
```

## Lambda実行環境の作成
* 関数名: APIGWHandsOn
* ランタイム: Java8
* ハンドラ: example.HelloPojo::handleRequest
## Lambdaのデプロイ
```bash
cd ~/environment/apigw-lambda-kdf-sample/
aws lambda update-function-code --function-name APIGWHandsOn --zip-file fileb://target/lambda-java-example-1.0-SNAPSHOT.jar --publish
```

## Lambdaのテスト
テストイベントを以下のように設定

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "values" : "20,30,40"
}

```

# API Gatewayを呼び出す
## APIの定義（Lambda backed）
* API名: APIGWHandsOn
* リソース: /metrics
* メソッド: POST
  * 統合タイプ: Lambda関数

## APIのテスト
テストイベントを以下のように設定

```json
{
  "firstName": "John",
  "lastName": "Doe",
  "values" : "20,30,40"
}

```

## APIのデプロイ
* ステージ名: prod
* ステージの種類: production

## デプロイしたAPIのテスト
API Gatewayマネジメントコンソールから API エンドポイントを確認
* 例: https://62pxxxnh23.execute-api.ap-northeast-1.amazonaws.com/prod/metrics

Cloud9ターミナルから以下のようなコマンドでテスト
```bash
curl -d '{"firstName": "John", "lastName": "Doe", "values" : "36,68,100,32" }' https://62pxxxnh23.execute-api.ap-northeast-1.amazonaws.com/prod/metrics
```



# Kinesis経由でS3にファイルを置く
## S3バケットの作成
* バケット名: お名前-apigwhandson （例: ohmurayu-apigwhandson)

## Kinesis Data Firehose(KDF)の作成
* Delivery Stream Name: APIGWHandsOnStream
  * HelloPojo.java の以下のコードで呼び出しています
```java
public class HelloPojo implements RequestHandler<RequestClass, ResponseClass>{ 

    private static String deliveryStreamName = "APIGWHandsOnStream";
```
* S3bucket: 先ほど作成したバケット名（お名前-apigwhandson)
* S3 prefix: raw
* S3 error prefix: err
* S3 buffer conditions
  * Buffer size: 1 MB
  * Buffer interval: 60 seconds
* IAM role: "create new or choose"
  * "新しいロールポリシーの作成"を指定して "許可"

## Lambdaの実行ロールにKinesis の呼び出しポリシーを追加する
* APIGWHandsOn-role-xxxxx のロールへ、"AmazonKinesisFirehoseFullAccess" ポリシーを追加

## Lambdaファンクションを修正（KDFへデータを送るように）
1. HelloPojo.java を以下のように修正（コメントアウトを戻す）
```java
    public ResponseClass handleRequest(RequestClass request, Context context){

        String time = OffsetDateTime.now().toString();
        firehoseClient = AmazonKinesisFirehoseClientBuilder.standard().build();
        putRecordIntoDeliveryStream(time + "," + request.values);
```

2. 作成したパッケージをデプロイ
```bash
cd ~/environment/apigw-lambda-kdf-sample/
mvn package
aws lambda update-function-code --function-name APIGWHandsOn --zip-file fileb://target/lambda-java-example-1.0-SNAPSHOT.jar --publish
```

## LambdaおよびAPIをテストしてファイルがS3に保存されることを確認
マネジメントコンソール(Lambda)およびCloud9ターミナル(curlでAPIを呼び出す)にて確認

## 後のテストのために継続的にデータを投入するようセットする
1. Cloud9エディタで injest.shを開きAPIエンドポイントを修正
```bash
#!/bin/bash
endpoint=https://62pxxxnh23.execute-api.ap-northeast-1.amazonaws.com/prod/metrics
```

2. Cloud9コンソールでinjest.shを実行する
```bash
cd ~/environment/apigw-lambda-kdf-sample
./injest.sh
{"greetings":"Hello John, Doe. time=2019-09-01T10:13:28.705Z, values=36,100,0,32"}
（以下１分ごとに実行されます）
```

# S3のファイルをAthenaでクエリする
## S3のファイルに対して外部テーブルを定義する
* データベースは "default" を選択する
* LOCATIONには自身が作成したs3バケットを指定する
* テーブル名には'-'を使用しないことを推奨
```sql
create external table apigw_handson (
  time string,
  val1  int,
  val2  int,
  val3  int,
  val4  int)  
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde' 
WITH SERDEPROPERTIES ("separatorChar" = ",", "escapeChar" = "\\") 
LOCATION 's3://ohmurayu-apigwhandson/raw/';
```

## Athenaでテーブルに対してクエリする
```sql
select * from apigw_handson;
```

# QuickSightで可視化する
## QuickSightにアクセス権を付与する
* N.Virginia リージョンを指定し、右上のメニューから"Manage QuickSight"を選択
* Security & Permissions: 先ほど作ったS3バケット (お名前-apigwhandson) へのアクセス許可を追加
## Athenaテーブルをソースとしたデータセットを作成する
* データソース名: APIGWHandsOn
* "time"の形式が文字列であるため、日付形式を指定して日付型とする
  * yyyy-MM-dd'T'HH:mm:ss.SSSZ と指定すること

## 分析を作成して可視化する
QuickSightで可視化

---
以上です。お疲れさまでした。



