package com.app.inventarisbarang.ui.pengguna

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.inventarisbarang.R
import com.app.inventarisbarang.databinding.ItemUserManageBinding
import com.app.inventarisbarang.model.User

class PenggunaAdapter(
    private var userList: List<User> = emptyList(),
    private val onItemClick: (User) -> Unit,
    private val onActionClick: ((User) -> Unit)? = null
) : RecyclerView.Adapter<PenggunaAdapter.UserViewHolder>() {

    fun submitList(newList: List<User>) {
        userList = newList
        notifyDataSetChanged()
    }

    inner class UserViewHolder(private val binding: ItemUserManageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvNamaUser.text = user.name.ifEmpty { user.username }
            val divisiText = user.divisi?.ifEmpty { "Divisi Operasional" } ?: "Divisi Operasional"
            binding.tvUsernameUser.text = "@${user.username} • $divisiText"

            val initials = if (user.name.isNotBlank()) {
                user.name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercaseChar() }
                    .joinToString("")
            } else {
                user.username.take(2).uppercase()
            }
            binding.tvAvatarInitial.text = initials.ifEmpty { "U" }

            val isAdmin = user.role.equals("admin", true)
            if (isAdmin) {
                binding.tvRoleTag.text = "ADMIN"
                binding.tvRoleTag.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, R.color.primary)
            } else {
                binding.tvRoleTag.text = "ANGGOTA"
                binding.tvRoleTag.backgroundTintList = ContextCompat.getColorStateList(binding.root.context, R.color.secondary)
            }

            val isActive = user.statusAktif ?: true
            binding.dotOnlineStatus.backgroundTintList = ContextCompat.getColorStateList(
                binding.root.context,
                if (isActive) R.color.secondary else R.color.text_muted
            )

            binding.btnUserAction.setOnClickListener {
                onActionClick?.invoke(user)
            }

            binding.root.setOnClickListener {
                onItemClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserManageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size
}
