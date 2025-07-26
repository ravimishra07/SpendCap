package com.ravi.samstudioapp.domain.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "bank_transactions",
    indices = [Index(value = ["amount", "bankName", "messageTime"], unique = true)]
)
data class BankTransaction(
    @PrimaryKey val messageTime: Long,
    val amount: Double,
    val bankName: String,
    val tags: String = "",
    val count: Int? = null,
    val category: String = "Other",
    val verified: Boolean = false,
    val deleted: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString() ?: "Other",
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(messageTime)
        parcel.writeDouble(amount)
        parcel.writeString(bankName)
        parcel.writeString(tags)
        parcel.writeValue(count)
        parcel.writeString(category)
        parcel.writeByte(if (verified) 1 else 0)
        parcel.writeByte(if (deleted) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BankTransaction> {
        override fun createFromParcel(parcel: Parcel): BankTransaction {
            return BankTransaction(parcel)
        }

        override fun newArray(size: Int): Array<BankTransaction?> {
            return arrayOfNulls(size)
        }
    }
} 