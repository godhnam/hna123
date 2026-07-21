-- =========================================================================
-- KHỞI TẠO HỆ THỐNG VÀ DỊCH VỤ
-- =========================================================================
local RunService = game:GetService("RunService")
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local CollectionService = game:GetService("CollectionService")
local TweenService = game:GetService("TweenService")
local TeleportService = game:GetService("TeleportService")
local HttpService = game:GetService("HttpService")
local Workspace = game:GetService("Workspace")

local LocalPlayer = Players.LocalPlayer
local playerGui = LocalPlayer:WaitForChild("PlayerGui")

-- Cấu hình
_G.AutoFarmChest = true
_G.AutoDarkbeard = true
local ChestTargetLimit = 70
local FarmSpeed = 300
local countChests = 0

local isWorld2 = (game.PlaceId == 444227218)
local isWorld3 = (game.PlaceId == 7449423635)

-- =========================================================================
-- GIAO DIỆN (UI) MARIS HUB FRAME CHEST
-- =========================================================================
local screenGui = Instance.new("ScreenGui", playerGui)
screenGui.Name = "MarisHub_ChestGUI"
screenGui.ResetOnSpawn = false

local mainFrame = Instance.new("Frame", screenGui)
mainFrame.Size = UDim2.new(0, 360, 0, 180)
mainFrame.Position = UDim2.new(0.5, -180, 0.5, -90)
mainFrame.BackgroundColor3 = Color3.fromRGB(15, 15, 15)
mainFrame.BackgroundTransparency = 0.2
mainFrame.BorderSizePixel = 0
mainFrame.Active = true
mainFrame.Draggable = true
Instance.new("UICorner", mainFrame).CornerRadius = UDim.new(0, 15)

local uiStroke = Instance.new("UIStroke", mainFrame)
uiStroke.Thickness = 4
uiStroke.ApplyStrokeMode = Enum.ApplyStrokeMode.Border

local function createGlowingText(text, position, fontSize)
    local label = Instance.new("TextLabel", mainFrame)
    label.Size = UDim2.new(1, 0, 0, 40)
    label.Position = position
    label.BackgroundTransparency = 1
    label.Text = text
    label.Font = Enum.Font.GothamBold
    label.TextSize = fontSize
    label.TextColor3 = Color3.fromRGB(255, 255, 255)
    
    local textStroke = Instance.new("UIStroke", label)
    textStroke.Color = Color3.fromRGB(0, 0, 0)
    textStroke.Thickness = 2
    
    return label
end

local title = createGlowingText("MARIS HUB", UDim2.new(0, 0, 0, 20), 28)
local status = createGlowingText("Auto Chest & Darkbeard Boss", UDim2.new(0, 0, 0, 70), 18)
local link = createGlowingText("https://discord.gg/vNWPpQUVuV", UDim2.new(0, 0, 0, 120), 16)

RunService.RenderStepped:Connect(function()
    local time = tick()
    uiStroke.Color = Color3.fromHSV(time % 3 / 3, 1, 1)
    title.TextColor3 = Color3.fromHSV((time * 0.5) % 1, 0.7, 1)
    local pulse = (math.sin(time * 3) + 1) / 2
    status.TextColor3 = Color3.new(pulse, 1, pulse)
    local glow = (math.sin(time * 1.5) + 1) / 2
    link.TextColor3 = Color3.fromRGB(100 + (glow * 155), 200, 255)
end)

-- =========================================================================
-- HỆ THỐNG GHI NHỚ SERVER (TRÁNH LẠI SERVER CŨ)
-- =========================================================================
local function getVisitedServers()
    local list = {}
    pcall(function()
        if readfile and isfile and isfile("visited_servers.txt") then
            local content = readfile("visited_servers.txt")
            for id in string.gmatch(content, "[^\n]+") do
                list[id] = true
            end
        end
    end)
    return list
end

local function saveVisitedServer(serverId)
    pcall(function()
        if writefile and readfile then
            local content = ""
            if isfile and isfile("visited_servers.txt") then
                content = readfile("visited_servers.txt")
            end
            local lines = {}
            for id in string.gmatch(content, "[^\n]+") do
                if id ~= serverId then table.insert(lines, id) end
            end
            table.insert(lines, serverId)
            if #lines > 50 then table.remove(lines, 1) end
            writefile("visited_servers.txt", table.concat(lines, "\n"))
        end
    end)
end

pcall(function() saveVisitedServer(game.JobId) end)

-- =========================================================================
-- REMOTE AN TOÀN
-- =========================================================================
local function getCommF()
    local remotes = ReplicatedStorage:WaitForChild("Remotes", 5)
    return remotes and remotes:WaitForChild("CommF_", 5)
end

-- =========================================================================
-- TỰ ĐỘNG CHỌN PHE (SET TEAM PIRATES)
-- =========================================================================
local function selectTeam()
    local teamName = "Pirates"
    print("Đang tự động chọn phe: " .. teamName)
    pcall(function()
        local CommF = getCommF()
        if CommF then CommF:InvokeServer("SetTeam", teamName) end
    end)
    pcall(function()
        local pg = LocalPlayer:FindFirstChildOfClass("PlayerGui")
        if pg then
            for _, v in ipairs(pg:GetDescendants()) do
                if v:IsA("TextButton") and (v.Name == teamName or string.find(v.Text, teamName)) then
                    local clicked = false
                    if getconnections then
                        for _, c in pairs(getconnections(v.MouseButton1Click)) do
                            c.Function()
                            clicked = true
                        end
                    end
                    if not clicked and firesignal then
                        firesignal(v.MouseButton1Click)
                        clicked = true
                    end
                    if not clicked then
                        pcall(function() v:Click() end)
                    end
                end
            end
        end
    end)
end

-- =========================================================================
-- HÀM DI CHUYỂN MƯỢT MÀ (ĐÃ FIX LỖI GIẬT LAG)
-- =========================================================================
local function bayDen(targetCFrame, speed)
    local character = LocalPlayer.Character
    if not character then 
        pcall(function() character = LocalPlayer.CharacterAdded:Wait() end)
    end
    if not character then return end
    
    local hrp = character:WaitForChild("HumanoidRootPart", 10)
    local humanoid = character:FindFirstChildOfClass("Humanoid")
    if not hrp or not humanoid then return end
    
    humanoid.Sit = false
    local distance = (targetCFrame.Position - hrp.Position).Magnitude
    if distance < 50 then
        hrp.CFrame = targetCFrame
        return
    end
    
    local duration = distance / speed
    local pathPart = Instance.new("Part")
    pathPart.Name = "TweenGhostFix"
    pathPart.Transparency = 1
    pathPart.Anchored = true
    pathPart.CanCollide = false
    pathPart.CFrame = hrp.CFrame
    pathPart.Size = Vector3.new(2, 2, 2)
    pathPart.Parent = workspace
    
    local noclipConnection
    noclipConnection = RunService.Stepped:Connect(function()
        if character then
            for _, part in ipairs(character:GetDescendants()) do
                if part:IsA("BasePart") then part.CanCollide = false end
            end
        end
    end)
    
    local heartbeatConnection
    heartbeatConnection = RunService.Heartbeat:Connect(function()
        if hrp and pathPart then
            hrp.CFrame = pathPart.CFrame
            hrp.AssemblyLinearVelocity = Vector3.new(0, 0, 0)
            hrp.AssemblyAngularVelocity = Vector3.new(0, 0, 0)
        end
    end)
    
    local tween = TweenService:Create(pathPart, TweenInfo.new(duration, Enum.EasingStyle.Linear), {CFrame = targetCFrame})
    tween:Play()
    tween.Completed:Wait()
    
    if heartbeatConnection then heartbeatConnection:Disconnect() end
    if noclipConnection then noclipConnection:Disconnect() end
    if pathPart then pathPart:Destroy() end
    hrp.CFrame = targetCFrame
end

-- =========================================================================
-- HỌP SERVER ÍT NGƯỜI
-- =========================================================================
local function hopLowServerFast()
    local CurrentPlaceId = game.PlaceId
    local apiUrl = "https://games.roblox.com/v1/games/" .. CurrentPlaceId .. "/servers/Public?sortOrder=Asc&limit=100"
    
    local function ListServers(cursor)
        local success, raw = pcall(function() return game:HttpGet(apiUrl .. ((cursor and "&cursor=" .. cursor) or "")) end)
        return success and HttpService:JSONDecode(raw) or nil
    end
    
    local Server
    local pageAttempts = 0
    local maxPages = 3
    local candidateServers = {}
    local visited = getVisitedServers()
    
    pcall(function()
        repeat 
            local Next
            local Servers = ListServers(Next)
            pageAttempts = pageAttempts + 1
            if Servers and Servers.data then
                for _, s in pairs(Servers.data) do
                    local playing = tonumber(s.playing)
                    local maxPlayers = tonumber(s.maxPlayers)
                    if s.id ~= game.JobId and not visited[s.id] and playing and maxPlayers and playing < (maxPlayers - 1) and playing >= 1 then
                        table.insert(candidateServers, s)
                    end
                end
                Next = Servers.nextPageCursor
            else
                break
            end
            task.wait(0.1)
        until #candidateServers > 0 or not Next or pageAttempts >= maxPages
    end)
    
    if #candidateServers > 0 then
        table.sort(candidateServers, function(a, b) return tonumber(a.playing) < tonumber(b.playing) end)
        Server = candidateServers[1]
    end
    
    if Server then
        saveVisitedServer(Server.id)
        pcall(function() ReplicatedStorage:WaitForChild("__ServerBrowser", 5):InvokeServer("teleport", Server.id) end)
        task.wait(3)
        pcall(function() TeleportService:TeleportToPlaceInstance(CurrentPlaceId, Server.id, LocalPlayer) end)
    end
end

-- =========================================================================
-- KIỂM TRA FIST OF DARKNESS HOẶC BOSS DARKBEARD
-- =========================================================================
local function hasFistOfDarkness()
    local bp = LocalPlayer:FindFirstChild("Backpack")
    local char = LocalPlayer.Character
    
    if bp then
        for _, item in ipairs(bp:GetChildren()) do
            if string.find(item.Name, "Fist of Darkness") then return true end
        end
    end
    if char then
        for _, item in ipairs(char:GetChildren()) do
            if string.find(item.Name, "Fist of Darkness") then return true end
        end
    end
    return false
end

local function getDarkbeardBoss()
    local enemies = Workspace:FindFirstChild("Enemies") or Workspace
    for _, v in ipairs(enemies:GetDescendants()) do
        if v.Name == "Darkbeard" and v:FindFirstChild("Humanoid") and v.Humanoid.Health > 0 then
            return v
        end
    end
    return nil
end

-- =========================================================================
-- HÀM TRANG BỊ VŨ KHÍ MELEE (CẬN CHIẾN)
-- =========================================================================
local function equipMelee()
    local char = LocalPlayer.Character
    local bp = LocalPlayer.Backpack
    if not char then return end
    
    -- Kiểm tra xem đã cầm Melee chưa
    local currentTool = char:FindFirstChildOfClass("Tool")
    if currentTool and currentTool.ToolTip == "Melee" then
        return currentTool
    end
    
    -- Tìm trong Backpack hoặc Character xem có tool nào là Melee không
    local function searchAndEquip(container)
        for _, tool in ipairs(container:GetChildren()) do
            if tool:IsA("Tool") and (tool.ToolTip == "Melee" or string.find(tool.Name, "Combat") or string.find(tool.Name, "Dark Step") or string.find(tool.Name, "Electro") or string.find(tool.Name, "Water Kung Fu") or string.find(tool.Name, "Dragon Breath") or string.find(tool.Name, "Superhuman") or string.find(tool.Name, "Death Step") or string.find(tool.Name, "Sharkman Karate") or string.find(tool.Name, "Electric Claw") or string.find(tool.Name, "Dragon Talon") or string.find(tool.Name, "Godhuman") or string.find(tool.Name, "Sanguine Art")) then
                if container == bp then
                    char.Humanoid:EquipTool(tool)
                end
                return tool
            end
        end
        return nil
    end
    
    local tool = searchAndEquip(char) or searchAndEquip(bp)
    -- Nếu không tìm thấy nhãn Melee rõ ràng, lấy đại công cụ đầu tiên làm phương án dự phòng
    if not tool and bp then
        local firstTool = bp:FindFirstChildOfClass("Tool")
        if firstTool then
            char.Humanoid:EquipTool(firstTool)
            tool = firstTool
        end
    end
    return tool
end

-- =========================================================================
-- XỬ LÝ TRIỆU HỒI & ĐÁNH BOSS DARKBEARD BẰNG MELEE
-- =========================================================================
local darkArenaCFrame = CFrame.new(3781.5, 23.4, -13904.3)

local function handleDarkbeardEvent()
    _G.AutoFarmChest = false
    print("⚔️ ĐÃ PHÁT HIỆN FIST OF DARKNESS HOẶC BOSS DARKBEARD! Tiến hành xử lý...")
    
    -- Bay đến Đảo Đen
    bayDen(darkArenaCFrame + Vector3.new(0, 15, 0), FarmSpeed)
    task.wait(1.5)
    
    -- Nếu có Fist of Darkness trong balo, trang bị và chạm vào bệ thờ triệu hồi
    if hasFistOfDarkness() then
        print("🗿 Đang trang bị Fist of Darkness để kích hoạt bệ thờ...")
        pcall(function()
            local bp = LocalPlayer.Backpack
            for _, item in ipairs(bp:GetChildren()) do
                if string.find(item.Name, "Fist of Darkness") then
                    LocalPlayer.Character.Humanoid:EquipTool(item)
                    break
                end
            end
        end)
        task.wait(1)
        
        bayDen(darkArenaCFrame + Vector3.new(0, 3, 0), 150)
        task.wait(1)
        
        pcall(function()
            local CommF = getCommF()
            if CommF then
                CommF:InvokeServer("Darkbeard", "Spawn")
            end
        end)
        task.wait(2)
    end
    
    -- Tiến hành đánh Boss Darkbeard bằng Melee với nhịp độ an toàn
    print("🛡️ Đang đánh Boss Darkbeard bằng Melee (Kiểm soát tốc độ an toàn)...")
    while _G.AutoDarkbeard do
        task.wait(0.35) -- Giãn cách nhịp đánh vừa phải, chống lỗi quét hệ thống
        
        local boss = getDarkbeardBoss()
        if boss and boss:FindFirstChild("HumanoidRootPart") and boss.Humanoid.Health > 0 then
            -- Bay lơ lửng sát cạnh Boss để đánh Melee chuẩn xác
            bayDen(boss.HumanoidRootPart.CFrame + Vector3.new(0, 10, 0), FarmSpeed)
            
            pcall(function()
                local meleeTool = equipMelee()
                if meleeTool and meleeTool:FindFirstChild("Activate") then
                    meleeTool:Activate()
                end
                
                -- Tự động bấm các phím kỹ năng Melee (Z, X, C) với khoảng nghỉ an toàn
                local vim = game:GetService("VirtualInputManager")
                for _, key in ipairs({Enum.KeyCode.Z, Enum.KeyCode.X, Enum.KeyCode.C}) do
                    vim:SendKeyEvent(true, key, false, game)
                    task.wait(0.05)
                    vim:SendKeyEvent(false, key, false, game)
                    task.wait(0.1)
                end
            end)
        else
            print("🎉 Boss Darkbeard đã bị hạ gục! Quay lại tiến trình săn rương...")
            task.wait(2)
            break
        end
    end
end

-- =========================================================================
-- HỆ THỐNG FARM RƯƠNG & GIÁM SÁT SỰ KIỆN LIÊN TỤC
-- =========================================================================
local function getNearestChest()
    local char = LocalPlayer.Character
    if not char or not char:FindFirstChild("HumanoidRootPart") then return nil end
    local myPos = char.HumanoidRootPart.Position
    local chests = CollectionService:GetTagged("_ChestTagged")
    local nearest = nil
    local shortest = math.huge
    
    for _, chest in ipairs(chests) do
        if (chest:IsA("BasePart") or chest:IsA("Model")) and not chest:GetAttribute("IsDisabled") then
            local chestPos = chest:GetPivot().Position
            local dist = (chestPos - myPos).Magnitude
            if dist < shortest then
                shortest = dist
                nearest = chest
            end
        end
    end
    return nearest
end

local function runFarmChest()
    local globalNoclip = RunService.Stepped:Connect(function()
        if _G.AutoFarmChest and LocalPlayer.Character then
            for _, part in ipairs(LocalPlayer.Character:GetDescendants()) do
                if part:IsA("BasePart") then part.CanCollide = false end
            end
        end
    end)
    
    task.spawn(function()
        while _G.AutoFarmChest do
            task.wait()
            
            if isWorld2 and (hasFistOfDarkness() or getDarkbeardBoss()) then
                _G.AutoFarmChest = false
                globalNoclip:Disconnect()
                handleDarkbeardEvent()
                break
            end
            
            local targetChest = getNearestChest()
            if targetChest then
                local chestPos = targetChest:GetPivot().Position
                bayDen(CFrame.new(chestPos), FarmSpeed)
                
                pcall(function()
                    if LocalPlayer.Character and LocalPlayer.Character:FindFirstChild("Humanoid") then
                        LocalPlayer.Character.Humanoid.Jump = true
                    end
                end)
                
                local waitTime = 0
                while not targetChest:GetAttribute("IsDisabled") and targetChest.Parent ~= nil and waitTime < 0.4 do
                    task.wait(0.05)
                    waitTime = waitTime + 0.05
                end
                
                countChests = countChests + 1
                print(string.format("🎒 Nhặt rương (%d/%d)", countChests, ChestTargetLimit))
                
                if countChests >= ChestTargetLimit then
                    globalNoclip:Disconnect()
                    _G.AutoFarmChest = false
                    print("🔄 Đã đạt giới hạn rương. Đang đổi server...")
                    while true do
                        hopLowServerFast()
                        task.wait(8)
                    end
                    break
                end
            else
                print("⚠️ Hết rương trên server này. Đang đổi server...")
                globalNoclip:Disconnect()
                _G.AutoFarmChest = false
                while true do
                    hopLowServerFast()
                    task.wait(8)
                end
                break
            end
        end
    end)
end

-- =========================================================================
-- KHỞI CHẠY CHƯƠNG TRÌNH CHÍNH
-- =========================================================================
task.spawn(function()
    task.wait(2)
    selectTeam() -- Tự động chọn phe Pirates
    task.wait(2)
    
    print("========== MARIS HUB: KHỞI ĐỘNG HOÀN TẤT (MELEE COMBAT) ==========")
    
    if isWorld2 and (hasFistOfDarkness() or getDarkbeardBoss()) then
        handleDarkbeardEvent()
    else
        runFarmChest()
    end
    
    while true do
        task.wait(0.5)
        if isWorld2 and _G.AutoDarkbeard and (hasFistOfDarkness() or getDarkbeardBoss()) then
            if _G.AutoFarmChest then
                _G.AutoFarmChest = false
                handleDarkbeardEvent()
            end
        end
    end
end)
