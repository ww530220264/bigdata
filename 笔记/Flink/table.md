# 动态表-Dynamic Tables

## 在流上定义表

![Continuous Non-Windowed Query](./image/append-mode.png)

## 连续查询

![Continuous Non-Windowed Query](./image/query-groupBy-window-cnt.png)

![Continuous Group-Window Query](./image/query-groupBy-window-cnt.png)

## 将表转换为流-Table to Stream Conversion

> Append-only stream

> Retract stream

![Continuous Non-Windowed Query](./image/undo-redo-mode.png)

> Upsert stream

![Continuous Non-Windowed Query](./image/redo-mode.png)

# Time Attributes

> Table程序需要在Streaming环境中指定响应的时间特性