package aggregators;

/**
 * Interface, which every data aggregator class should implement to be used by data aggregation service
 */
internal interface IDataAggregator {
    fun aggregate()
}
