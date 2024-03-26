/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package ru.dimon6018.neko11

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NekoDialog(context: Context) :
    Dialog(context, android.R.style.Theme_Material_Dialog_NoActionBar) {
    private val mAdapter: Adapter

    init {
        val view = RecyclerView(getContext())
        mAdapter = Adapter(getContext())
        view.layoutManager = GridLayoutManager(getContext(), 2)
        view.adapter = mAdapter
        val dp = context.resources.displayMetrics.density
        val pad = (16 * dp).toInt()
        view.setPadding(pad, pad, pad, pad)
        setContentView(view)
    }

    private fun onFoodSelected(food: Food) {
        val prefs = PrefState(context)
        val currentState = prefs.foodState
        if (currentState == 0 && food.type != 0) {
            NekoService.scheduleFoodWork(context, food.getInterval(context).toInt())
        }
        prefs.foodState = food.type
        dismiss()
    }

    private inner class Adapter(private val mContext: Context) : RecyclerView.Adapter<Holder>() {
        private val mFoods = ArrayList<Food>()

        init {
            val foods = context.resources.getIntArray(R.array.food_names)
            // skip food 0, you can't choose it
            for (i in 1 until foods.size) {
                mFoods.add(Food(i))
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.food_layout, parent, false)
            )
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val food = mFoods[position]
            (holder.itemView.findViewById<View>(R.id.icon) as ImageView)
                .setImageIcon(food.getIcon(mContext))
            (holder.itemView.findViewById<View>(R.id.text) as TextView).text =
                food.getName(mContext)
            holder.itemView.setOnClickListener {
                onFoodSelected(
                    mFoods[holder.adapterPosition]
                )
            }
        }

        override fun getItemCount(): Int {
            return mFoods.size
        }
    }

    class Holder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
}
