package encore.creation

import encore.utils.types.Report
import forum.domain.profile.model.Profile
import forum.mongo.collection.UserAccount
import forum.mongo.collection.UserId

class BlankUserCreationFactory: UserCreationFactory {
    override fun userId(isAdmin: Boolean): UserId {
        TODO("Not yet implemented")
    }

    override fun account(
        userId: UserId,
        username: String,
        password: String,
        email: String
    ): UserAccount {
        TODO("Not yet implemented")
    }

    override fun profile(
        userId: UserId,
        username: String
    ): Profile {
        TODO("Not yet implemented")
    }

    override fun updateServerObjects(
        account: UserAccount
    ): Report {
        TODO("Not yet implemented")
    }
}
