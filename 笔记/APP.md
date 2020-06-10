```sql
create external table if not exists subject(
          user_id bigint comment '学生ID',
          class_id int comment '班级ID',
          paper_id int comment '试卷ID',
          paper_calss int comment '试卷科目',
          subject_id int comment '题目ID',
          subject_type int comment '试卷类型',
          subject_category_id int comment '题目分类',
          subject_score int comment '题目分数',
          subject_answer int comment '作答结果',
          datetime timestamp comment '作答时间')
          comment '学生答题表' partitioned by (day string comment '采集日期')
          row format delimited
          fields terminated by ','
          stored as textfile
          location '/flume/edu/20200609';
```

