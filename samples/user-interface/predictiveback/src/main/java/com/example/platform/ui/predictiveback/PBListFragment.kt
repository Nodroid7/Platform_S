/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.predictiveback

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.platform.ui.predictiveback.databinding.FragmentAnimationListBinding

class PBListFragment : Fragment() {

    private var _binding: FragmentAnimationListBinding? = null
    private val binding
        get() = checkNotNull(_binding){
            "Cannot access binding in PBListFragment because it is null."
        }

    private val pbViewModel: PBViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAnimationListBinding.inflate(inflater, container, false)
        binding.pbRecyclerView.layoutManager = LinearLayoutManager(context)

        val animations = pbViewModel.animations
        val adapter = PBListAdapter(animations)
        binding.pbRecyclerView.adapter = adapter

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}