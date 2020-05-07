package com.ww.jvm;

/**
 * @author wangwei@huixiangtech.cn
 * @version 1.0
 * @className gc
 * @description TODO
 * @date 2020/4/23-15:04
 **/
public class gc {
    private static final int _1MB = 1024*1024;
    /*
     * -Xms 堆初始大小
     * -Xmx 堆最大大小
     * -Xmn 新生代大小
     * -XX:+PrintGCDetails
     * -XX:+SurvivorRatio
     * -XX:PreternureSizeThreshold     直接晋升老年代对象的大小,大于这个参数的对象直接在老年代分配
     * -XX:MaxTenuringThreshold    晋升老年代的对象年龄,对象在每一次Minor GC后年龄增加一岁,超过这个值之后进入到老年代,默认为15
     * -XX:NativeMemoryTracking=detail
     *      使用命令 jcmd pid VM.native_memory detail配合查看JVM相关情况
     * 收集器分类:
     *      新生代:Serial/ParNew/Parallel Scavenge
     *      老年代:CMS/Serial Old/Parallel Old
     *      总结:
     *          Serial:最古老,串行,效率高,会暂停用户线程(Stop The World)
     *          Serial Old
     *          ParNew:Serial的多线程版本,多核CPU上使用(Stop The World)
     *          Parallel Scavenge:更关注CPU吞吐量和停顿时间,可以配置这两个参数.
     *          CMS:初始标记(Stop the World,只标记和GC Root直接关联的对象)--并发标记(和用户线程一起工作)--重新标记(Stop the World,修正在并发标记期间应用程序运行造成的变动的对象)--并发清除(和用户线程一起工作)
     *              并发收集,停顿低.但是产生大量空间碎片,并发期间会降低系统吞吐量
     *              如果CMS失败,则使用Serial Old串行收集
     * -XX:UseSerialGC  新生代和老年代都使用串行回收器(单线程,新生代复制,老年代标记-整理)
     * -XX:UseParNew    新生代并行(复制),老年代串行(标记-整理)
     * -XX:UseParallelGC    新生代使用Parallel Scavenge收集器,并行收集,老年代串行
     * -XX:UseParallelOldGC     新生代和老年代都使用并行收集器
     * -XX:UseConcMarkSweepGC   老年代使用并发标记清除,新生代使用ParNew收集器(默认关闭)
     *      -XX:UseCMSCompactAtFullCollection   FullGC之后,进行一次整理,整理过程是独占的,会引起停顿时间变长,仅在CMS收集器下生效
     *      -XX:ParallelCMSThreads  设置并行GC时进行内存回收的线程数量
     *
     *
     *
     **/
    public static void main(String[] args) throws InterruptedException {
        byte[] b1 = new byte[2* _1MB];
        byte[] b2 = new byte[2* _1MB];
        byte[] b3 = new byte[2* _1MB];
        byte[] b4 = new byte[4* _1MB];
    }
}
