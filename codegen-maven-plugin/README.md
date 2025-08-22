### install the plugin in our local repository
` mvn clean install `
### execute the plugin
`mvn groupId:artifactId:version:goal`
now in our case is: 实测，不加version也可以,命名规范**-maven-plugin，但是使用mvn goalPrefix:goal的方式还是不行，最后只能在setting.xml中加上了` <pluginGroup>com.yifan</pluginGroup>`
`mvn com.yifan:codegen-maven-plugin:0.0.1-SNAPSHOT:dependency-counter`
