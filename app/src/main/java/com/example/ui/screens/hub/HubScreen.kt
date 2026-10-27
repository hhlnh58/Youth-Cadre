package com.example.ui.screens.hub

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.CadreConnectivityHubCard
import com.example.ui.theme.*
import com.example.ui.util.CadreSystemIntegrations
import com.example.ui.viewmodel.CadreViewModel

@Composable
fun HubScreen(
    viewModel: CadreViewModel
) {
    var selectedTab by remember { mutableStateOf("PEOPLE") } // PEOPLE, VAULT, DECISIONS, NOTES, LINKS, WORKSPACES

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Tri thức & Điều hành",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = (-0.3).sp
            ),
            color = TextPrimary
        )
        Text(
            "Danh bạ lãnh đạo, kho văn bản & quyết định chỉ đạo",
            fontSize = 12.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CleanHubTabPill("Nhân sự", Icons.Default.Groups, selectedTab == "PEOPLE") { selectedTab = "PEOPLE" }
            CleanHubTabPill("Kho tài liệu", Icons.Default.FolderSpecial, selectedTab == "VAULT") { selectedTab = "VAULT" }
            CleanHubTabPill("Quyết định", Icons.Default.Gavel, selectedTab == "DECISIONS") { selectedTab = "DECISIONS" }
            CleanHubTabPill("Ghi chú", Icons.Default.NoteAlt, selectedTab == "NOTES") { selectedTab = "NOTES" }
            CleanHubTabPill("Liên kết", Icons.Default.Link, selectedTab == "LINKS") { selectedTab = "LINKS" }
            CleanHubTabPill("Workspace", Icons.Default.Domain, selectedTab == "WORKSPACES") { selectedTab = "WORKSPACES" }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                "PEOPLE" -> CleanPeopleTab(viewModel)
                "VAULT" -> CleanVaultTab(viewModel)
                "DECISIONS" -> CleanDecisionsTab(viewModel)
                "NOTES" -> CleanNotesTab(viewModel)
                "LINKS" -> CleanLinksTab(viewModel)
                "WORKSPACES" -> CleanWorkspacesTab(viewModel)
            }
        }
    }
}

@Composable
fun CleanHubTabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) NavyDeep else PureWhite,
        border = BorderStroke(1.dp, if (isSelected) NavyDeep else BorderLight),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}

@Composable
fun CleanPeopleTab(viewModel: CadreViewModel) {
    val people by viewModel.people.collectAsState()
    val context = LocalContext.current
    var editingPerson by remember { mutableStateOf<PersonEntity?>(null) }
    var showPersonDialog by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (uri != null) {
            val contact = CadreSystemIntegrations.resolveContact(context, uri)
            if (contact != null && contact.name.isNotBlank()) {
                viewModel.savePerson(
                    id = null,
                    fullName = contact.name.trim(),
                    position = "Cán bộ phối hợp",
                    unit = "Đoàn cơ sở / Cơ quan",
                    phone = contact.phone.trim(),
                    email = "",
                    notes = "Nhập từ Danh bạ điện thoại",
                    isKeyLeader = false
                )
                Toast.makeText(context, "Đã thêm đồng chí ${contact.name} vào danh bạ!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Không trích xuất được thông tin danh bạ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchContactPicker = {
        try {
            val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            contactPickerLauncher.launch(pickIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể mở danh bạ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${people.size} cán bộ & lãnh đạo", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { launchContactPicker() },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, NavyMedium.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyDeep)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Từ danh bạ máy", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NavyDeep)
                }

                Button(
                    onClick = {
                        editingPerson = null
                        showPersonDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (people.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(26.dp))
                    }
                    Text("Danh bạ nhân sự trống", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Chưa có thông tin cán bộ hoặc lãnh đạo trong danh bạ.\nĐồng chí có thể nhập trực tiếp từ Danh bạ điện thoại của máy hoặc tạo mới thủ công.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { launchContactPicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Từ Danh bạ máy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                editingPerson = null
                                showPersonDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NavyDeep)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = NavyDeep)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thêm thủ công", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDeep)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(people, key = { it.id }) { person ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingPerson = person
                                showPersonDialog = true
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(
                            1.dp,
                            if (person.isKeyLeader) ChampagneGold.copy(alpha = 0.5f) else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (person.isKeyLeader) ChampagneGold.copy(alpha = 0.15f) else Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = person.fullName.take(1),
                                    fontWeight = FontWeight.Black,
                                    color = if (person.isKeyLeader) ChampagneGold else NavyDeep,
                                    fontSize = 16.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(person.fullName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                    if (person.isKeyLeader) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = ChampagneGold.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                "Thường trực",
                                                fontSize = 9.sp,
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text("${person.position} • ${person.unit}", fontSize = 11.sp, color = TextSecondary)
                                if (person.phone.isNotBlank()) {
                                    Text(person.phone, fontSize = 10.5.sp, color = NavyMedium, fontWeight = FontWeight.Medium)
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                if (person.phone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${person.phone}"))
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = "Gọi", tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        editingPerson = person
                                        showPersonDialog = true
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPersonDialog) {
        PersonDetailDialog(
            person = editingPerson,
            onDismiss = {
                showPersonDialog = false
                editingPerson = null
            },
            onSave = { id, name, pos, unit, phone, email, notes, isKeyLeader ->
                viewModel.savePerson(id, name, pos, unit, phone, email, notes, isKeyLeader)
                showPersonDialog = false
                editingPerson = null
                Toast.makeText(context, if (id == null) "Đã thêm đồng chí $name!" else "Đã cập nhật thông tin đồng chí $name!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { personToDelete ->
                viewModel.deletePerson(personToDelete)
                showPersonDialog = false
                editingPerson = null
                Toast.makeText(context, "Đã xóa cán bộ khỏi danh bạ", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CleanVaultTab(viewModel: CadreViewModel) {
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current
    var showAddDocDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${documents.size} văn bản lưu trữ", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = { showAddDocDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (documents.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(26.dp))
                    }
                    Text("Kho văn bản trống", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Chưa có tài liệu hoặc văn bản được lưu trữ.\nNhấn 'Thêm văn bản' để số hóa hồ sơ điều hành.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { showAddDocDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm văn bản", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(documents, key = { it.id }) { doc ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                if (doc.fileUrl.isNotBlank()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.fileUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text(
                                        doc.docType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyDeep,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(doc.versionName, fontSize = 10.sp, color = ChampagneGold, fontWeight = FontWeight.Medium)
                            }

                            Text(doc.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text("${doc.issuingBody} • ${doc.issueDate}", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showAddDocDialog) {
        AddDocumentDialog(
            onDismiss = { showAddDocDialog = false },
            onSave = { title, docType, docNum, summary, fileUrl, version ->
                viewModel.saveDocument(null, title, docType, docNum, summary, fileUrl, version)
                showAddDocDialog = false
            }
        )
    }
}

@Composable
fun CleanDecisionsTab(viewModel: CadreViewModel) {
    val decisions by viewModel.decisions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${decisions.size} quyết định chỉ đạo", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ghi nhận", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (decisions.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(26.dp))
                    }
                    Text("Chưa có quyết định chỉ đạo", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Lưu trữ các kết luận chỉ đạo, quy định và bài học kinh nghiệm điều hành.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ghi nhận quyết định", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(decisions, key = { it.id }) { dec ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(dec.decisionMaker, color = ChampagneGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(dec.date, color = TextSecondary, fontSize = 10.sp)
                            }

                            Text(dec.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(dec.content, color = TextSecondary, fontSize = 11.sp)

                            if (dec.workDnaLessons.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Kinh nghiệm: ${dec.workDnaLessons}",
                                        fontSize = 11.sp,
                                        color = NavyDeep,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddDecisionDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, maker, dna ->
                viewModel.saveDecision(null, title, content, maker, dna)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CleanNotesTab(viewModel: CadreViewModel) {
    val notes by viewModel.notes.collectAsState()
    var showAddNoteDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${notes.size} ghi chú", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = { showAddNoteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ghi chú", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (notes.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NoteAlt, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(26.dp))
                    }
                    Text("Chưa có ghi chú nào", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Lưu lại nhanh các ý kiến chỉ đạo, lưu ý công tác cơ sở hoặc nhắc nhở.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { showAddNoteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tạo ghi chú mới", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                if (note.isPinned) {
                                    Icon(Icons.Default.PushPin, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(note.content, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        AddNoteDialog(
            onDismiss = { showAddNoteDialog = false },
            onSave = { title, content, cat, isPinned ->
                viewModel.saveNote(null, title, content, cat, isPinned)
                showAddNoteDialog = false
            }
        )
    }
}

@Composable
fun CleanLinksTab(viewModel: CadreViewModel) {
    val links by viewModel.quickLinks.collectAsState()
    val context = LocalContext.current
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<QuickLinkEntity?>(null) }
    var deleteCandidate by remember { mutableStateOf<QuickLinkEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${links.size} liên kết nhanh", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = {
                    editingLink = null
                    showAddLinkDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (links.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                shape = RoundedCornerShape(20.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(26.dp))
                    }
                    Text("Chưa có liên kết nào", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(
                        "Lưu lại các đường link điều hành, cổng thông tin Đoàn hoặc biểu mẫu.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            editingLink = null
                            showAddLinkDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm liên kết", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(links, key = { it.id }) { link ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = PureWhite,
                        border = BorderStroke(1.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .clickable {
                                        try {
                                            val uri = Uri.parse(if (!link.url.startsWith("http://") && !link.url.startsWith("https://")) "https://${link.url}" else link.url)
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = NavyDeep, modifier = Modifier.size(18.dp))
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        try {
                                            val uri = Uri.parse(if (!link.url.startsWith("http://") && !link.url.startsWith("https://")) "https://${link.url}" else link.url)
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(link.title, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                    if (link.category.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                link.category,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NavyDeep,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }
                                }
                                Text(link.description.ifEmpty { link.url }, fontSize = 11.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            IconButton(
                                onClick = {
                                    editingLink = link
                                    showAddLinkDialog = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = NavyDeep, modifier = Modifier.size(15.dp))
                            }

                            IconButton(
                                onClick = { deleteCandidate = link },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Xóa", tint = CoralUrgent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLinkDialog) {
        AddLinkDialog(
            initialLink = editingLink,
            onDismiss = {
                showAddLinkDialog = false
                editingLink = null
            },
            onSave = { id, title, url, cat, desc ->
                viewModel.saveQuickLink(id = id, title = title, url = url, category = cat, desc = desc)
                showAddLinkDialog = false
                editingLink = null
            }
        )
    }

    if (deleteCandidate != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Xác nhận xóa liên kết?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Đồng chí có chắc chắn muốn xóa liên kết \"${deleteCandidate?.title}\"?", fontSize = 13.sp, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate?.let { viewModel.deleteQuickLink(it) }
                        deleteCandidate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralUrgent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Xác nhận xóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Hủy", color = TextSecondary)
                }
            },
            containerColor = PureWhite
        )
    }
}

@Composable
fun CleanWorkspacesTab(viewModel: CadreViewModel) {
    val workspaces by viewModel.workspaces.collectAsState()
    val currentWsId by viewModel.currentWorkspaceId.collectAsState()
    var showAddWsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${workspaces.size} không gian làm việc", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
            Button(
                onClick = { showAddWsDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tạo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(workspaces, key = { it.id }) { ws ->
                val isSelected = ws.id == currentWsId
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shapeRadius = 14.dp,
                    borderBrush = Brush.horizontalGradient(
                        if (isSelected) listOf(LuminousCyan, Color(0x30FFFFFF))
                        else listOf(Color(0x25FFFFFF), Color(0x10FFFFFF))
                    ),
                    onClick = { viewModel.setWorkspace(ws.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) LuminousCyan else Color(0x25FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Domain, contentDescription = null, tint = if (isSelected) VoidBlack else TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(ws.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(ws.organization, fontSize = 11.sp, color = TextSecondary)
                        }

                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddWsDialog) {
        AddWorkspaceDialog(
            onDismiss = { showAddWsDialog = false },
            onSave = { name, org, desc ->
                viewModel.createWorkspace(name, org, desc)
                showAddWsDialog = false
            }
        )
    }
}

@Composable
fun PersonDetailDialog(
    person: PersonEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        fullName: String,
        position: String,
        unit: String,
        phone: String,
        email: String,
        notes: String,
        isKeyLeader: Boolean
    ) -> Unit,
    onDelete: ((PersonEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    var fullName by remember(person?.id) { mutableStateOf(person?.fullName ?: "") }
    var position by remember(person?.id) { mutableStateOf(person?.position ?: "Cán bộ") }
    var unit by remember(person?.id) { mutableStateOf(person?.unit ?: "Ban Thường vụ") }
    var phone by remember(person?.id) { mutableStateOf(person?.phone ?: "") }
    var email by remember(person?.id) { mutableStateOf(person?.email ?: "") }
    var notes by remember(person?.id) { mutableStateOf(person?.notes ?: "") }
    var isKeyLeader by remember(person?.id) { mutableStateOf(person?.isKeyLeader ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (person == null) "Thêm Nhân sự mới" else "Chỉnh sửa Cán bộ",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = position,
                    onValueChange = { position = it },
                    label = { Text("Chức vụ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Đơn vị / Cơ quan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email liên hệ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú công tác") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ủy viên BTV / Thường trực", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Hiển thị nổi bật trong danh bạ", fontSize = 11.sp, color = TextSecondary)
                    }
                    Switch(
                        checked = isKeyLeader,
                        onCheckedChange = { isKeyLeader = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ChampagneGold, checkedTrackColor = NavyDeep)
                    )
                }

                if (person != null && phone.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(dialIntent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = EmeraldSuccess)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gọi điện", fontSize = 11.sp, color = EmeraldSuccess)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
                                    context.startActivity(smsIntent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(14.dp), tint = NavyDeep)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nhắn tin", fontSize = 11.sp, color = NavyDeep)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isNotBlank()) {
                        onSave(
                            person?.id,
                            fullName.trim(),
                            position.trim(),
                            unit.trim(),
                            phone.trim(),
                            email.trim(),
                            notes.trim(),
                            isKeyLeader
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
            ) {
                Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (person != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(person) }
                    ) {
                        Text("Xóa", color = CoralUrgent, fontWeight = FontWeight.Bold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Hủy", color = TextSecondary)
                }
            }
        },
        containerColor = Color(0xF8FFFFFF)
    )
}

@Composable
fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, docType: String, docNum: String, summary: String, fileUrl: String, version: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var docType by remember { mutableStateOf("KẾ HOẠCH") }
    var fileUrl by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm Văn bản", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề văn bản *") })
                OutlinedTextField(value = docType, onValueChange = { docType = it }, label = { Text("Loại văn bản") })
                OutlinedTextField(value = fileUrl, onValueChange = { fileUrl = it }, label = { Text("Đường dẫn tệp (Google Drive)") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, docType, "", "", fileUrl, "Bản chính thức") },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = TextSecondary) } },
        containerColor = Color(0xF8FFFFFF)
    )
}

@Composable
fun AddDecisionDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, maker: String, dna: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var dna by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi nhận Quyết định & DNA", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề quyết định *") })
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Nội dung quyết định") })
                OutlinedTextField(value = dna, onValueChange = { dna = it }, label = { Text("Bài học kinh nghiệm") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onSave(title, content, "Ban Thường vụ", dna) },
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold)
            ) { Text("Lưu", color = VoidBlack, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = TextSecondary) } },
        containerColor = Color(0xF8FFFFFF)
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, cat: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ghi chú mới", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Tiêu đề") })
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Nội dung") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (content.isNotBlank()) onSave(title.ifEmpty { "Ghi chú nhanh" }, content, "QUICK", false) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = TextSecondary) } },
        containerColor = Color(0xF8FFFFFF)
    )
}

@Composable
fun AddLinkDialog(
    initialLink: QuickLinkEntity? = null,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, url: String, cat: String, desc: String) -> Unit
) {
    var title by remember { mutableStateOf(initialLink?.title ?: "") }
    var url by remember { mutableStateOf(initialLink?.url ?: "") }
    var category by remember { mutableStateOf(initialLink?.category ?: "Hệ thống") }
    var desc by remember { mutableStateOf(initialLink?.description ?: "") }

    val categories = listOf("Cổng thông tin", "Google Drive", "Biểu mẫu", "Hệ thống", "Tiện ích")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialLink != null) "Cập nhật Liên kết" else "Thêm Liên kết mới",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên liên kết *") },
                    placeholder = { Text("VD: Google Drive Ban Thường vụ") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Địa chỉ URL (https://...) *") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Mô tả / Ghi chú") },
                    placeholder = { Text("Kho tài liệu dùng chung...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Nhóm liên kết:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSel = category == cat
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) NavyDeep else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) NavyDeep else BorderLight),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                        ) {
                            Text(
                                cat,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                        onSave(initialLink?.id, title.trim(), fixedUrl.trim(), category, desc.trim())
                    }
                },
                enabled = title.isNotBlank() && url.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (initialLink != null) "Cập nhật" else "Lưu", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = TextSecondary) }
        },
        containerColor = PureWhite
    )
}

@Composable
fun AddWorkspaceDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, org: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("Đoàn TNCS Hồ Chí Minh") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo Workspace", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên Workspace *") })
                OutlinedTextField(value = org, onValueChange = { org = it }, label = { Text("Tổ chức trực thuộc") })
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, org, "") },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) { Text("Tạo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = TextSecondary) } },
        containerColor = Color(0xF8FFFFFF)
    )
}
