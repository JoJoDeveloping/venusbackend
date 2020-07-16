package venusbackend.simulator

interface Memory {
    fun removeByte(address: Number)
    suspend fun loadByte(address: Number): Int
    suspend fun loadHalfWord(addr: Number): Int
    suspend fun loadWord(addr: Number): Int
    suspend fun loadLong(addr: Number): Long
    fun storeByte(addr: Number, value: Number)
    fun storeHalfWord(addr: Number, value: Number)
    fun storeWord(addr: Number, value: Number)
    fun storeLong(addr: Number, value: Number)
}