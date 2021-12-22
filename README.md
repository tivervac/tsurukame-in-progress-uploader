# Upload exported Tsurukame local database to WaniKani

* Make sure you have [Java 17](https://adoptium.net/?variant=openjdk17) installed
* Make sure you have a WaniKani API token with the `assignments:start` and `reviews:create` permissions
* Open Tsurukame, go to `Settings` and press `Export local database`
* Clone this repo
* Run `./mvnw clean install` and wait for it to finish
* Run `java -jar target/tsurukame-in-progress-uploader-1.0.0-SNAPSHOT-jar-with-dependencies.jar <token> <db-file>`
  * In which `<token>` is your WaniKani API token
  * In which `<db-file>` is the path (absolute or relative) to the exported db file

# DB exploring

You can view the exported local database in a tool like [SQLite Browser](https://sqlitebrowser.org/). However, a lot of the information is saved as a [protobuf](https://developers.google.com/protocol-buffers/docs/proto3) binary message. If you've followed the README's in Tsurukame's `proto` and `sqlite3-extension` folder, you should now have an sqlite3 extension you can use. You can now view the previous messages by opening the database from the command line instead using `sqlite3 local-cache.db`. There you can query the deserialized data using commands like `SELECT proto("Progress",pb) FROM pending_progress LIMIT 1;` in which `Progress` is the name of the protobuf message and `pb` is the sqlite column.

# Protobuf generation

This repo includes `proto/WanikaniApi.java`. This was generated from [Tsurukame's protobuf definition](https://github.com/davidsansome/tsurukame/blob/dd76410f59f0e5a098d76a32b2d2a3f1019441f4/proto/wanikani_api.proto) by running `protoc --java_out=. --experimental_allow_proto3_optional wanikani_api.proto`. If you try this at a later version of Tsurukame, you might have to regenerate this file.
