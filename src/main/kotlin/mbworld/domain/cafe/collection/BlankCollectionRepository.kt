package mbworld.domain.cafe.collection

class BlankCollectionRepository: CollectionRepository {
    override suspend fun getSpaces(): Result<List<Space>> = TODO("NO OPERATION")
    override suspend fun getSections(): Result<List<Section>> = TODO("NO OPERATION")
    override suspend fun getSpace(spaceId: String): Result<Space> {
        TODO("Not yet implemented")
    }

    override suspend fun getSection(sectionId: String): Result<Section> {
        TODO("Not yet implemented")
    }

    override suspend fun incrementTopicCount(sectionId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun decrementTopicCount(sectionId: String): Result<Unit> {
        TODO("Not yet implemented")
    }
}
