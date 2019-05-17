sbt 'run -a interbounds -D../tests/demo/ -Sledb1.* "ledb1.ledb([I[II)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb2.* "ledb2header.ledb([I[I)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb2.* "ledb2.ledb([I[I)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb2a.* "ledb2a.ledb([I[I)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb2b.* "ledb2b.ledb([I[I)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb2c.* "ledb2c.ledb(Ljava/util/List;Ljava/util/ArrayList;)I"'
sbt 'run -a interbounds -D../tests/demo/ -Sledb3.* "ledb3.ledb([I[I)I"'  # control-flow issue
sbt 'run -a interbounds -D../tests/demo/ -Sledb4.* "ledb4.ledb(Ljava/util/List;Ljava/util/ArrayList;)V"' # breaks on cast

sbt 'run -a interbounds -D../tests/demo/ -SBlogger.* "Blogger.m()I"'
