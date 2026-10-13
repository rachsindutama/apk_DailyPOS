package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.min

/**
 * Reusable, clean, compact, and professional pagination control bar for lists.
 */
@Composable
fun PaginationControlBar(
    currentPage: Int,
    totalPages: Int,
    totalItems: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemName: String = "produk",
    pageSizeOptions: List<Int> = listOf(6, 12, 24, 48),
    onPageSizeChange: ((Int) -> Unit)? = null
) {
    if (totalItems <= 0) return

    val actualTotalPages = if (totalPages < 1) 1 else totalPages
    val clampedPage = currentPage.coerceIn(1, actualTotalPages)
    val startIndex = ((clampedPage - 1) * pageSize) + 1
    val endIndex = min(clampedPage * pageSize, totalItems)

    var pageDropdownExpanded by remember { mutableStateOf(false) }
    var pageSizeDropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pagination_control_bar"),
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Summary text
            Text(
                text = "$startIndex-$endIndex dari $totalItems $itemName",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            // Right: Page Size Dropdown + Prev / Page / Next controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Page Size Dropdown
                if (onPageSizeChange != null) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .clickable { pageSizeDropdownExpanded = true }
                                .testTag("page_size_dropdown_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$pageSize/hal",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = pageSizeDropdownExpanded,
                            onDismissRequest = { pageSizeDropdownExpanded = false }
                        ) {
                            pageSizeOptions.forEach { sizeOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$sizeOption per halaman",
                                            fontWeight = if (pageSize == sizeOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (pageSize == sizeOption) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        onPageSizeChange(sizeOption)
                                        pageSizeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Prev Button
                IconButton(
                    onClick = { onPageChange(clampedPage - 1) },
                    enabled = clampedPage > 1,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("pagination_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Sebelumnya",
                        tint = if (clampedPage > 1) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Page Indicator Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .height(26.dp)
                            .clickable { pageDropdownExpanded = true }
                            .testTag("page_selector_dropdown_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "$clampedPage / $actualTotalPages",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = pageDropdownExpanded,
                        onDismissRequest = { pageDropdownExpanded = false }
                    ) {
                        (1..actualTotalPages).forEach { pageNum ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Halaman $pageNum",
                                        fontWeight = if (pageNum == clampedPage) FontWeight.Bold else FontWeight.Normal,
                                        color = if (pageNum == clampedPage) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                                        fontSize = 11.sp
                                    )
                                },
                                onClick = {
                                    onPageChange(pageNum)
                                    pageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Next Button
                IconButton(
                    onClick = { onPageChange(clampedPage + 1) },
                    enabled = clampedPage < actualTotalPages,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("pagination_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Berikutnya",
                        tint = if (clampedPage < actualTotalPages) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Helper extension to slice a list into a paginated subset safely.
 */
fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    if (isEmpty() || pageSize <= 0) return emptyList()
    val totalPages = maxOf(1, ceil(size.toDouble() / pageSize).toInt())
    val clampedPage = page.coerceIn(1, totalPages)
    val fromIndex = (clampedPage - 1) * pageSize
    val toIndex = minOf(fromIndex + pageSize, size)
    return if (fromIndex in 0 until size) {
        subList(fromIndex, toIndex)
    } else {
        emptyList()
    }
}
