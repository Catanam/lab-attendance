package com.example.attendance.web.admin;

import com.example.attendance.domain.academic.Room;
import com.example.attendance.domain.academic.RoomRepository;
import com.example.attendance.domain.device.Device;
import com.example.attendance.domain.device.DeviceRepository;
import com.example.attendance.domain.device.DeviceStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Controller
@RequestMapping("/devices")
public class DeviceAdminController {
    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;

    public DeviceAdminController(DeviceRepository deviceRepository, RoomRepository roomRepository) {
        this.deviceRepository = deviceRepository;
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("devices", deviceRepository.findAllWithRoom());
        return "devices/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("rooms", roomRepository.findAll());
        return "devices/new";
    }

    @PostMapping("/new")
    public String create(@RequestParam UUID roomId, RedirectAttributes ra) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            ra.addFlashAttribute("error", "Room not found.");
            return "redirect:/devices/new";
        }

        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        String secret = HexFormat.of().formatHex(secretBytes);

        Device device = new Device(room, secret);
        deviceRepository.save(device);

        ra.addFlashAttribute("success",
            "Device created: " + device.getId() + " — secret: " + secret +
            " (copy now, not shown again)");
        return "redirect:/devices";
    }

    @PostMapping("/{id}/revoke")
    public String revoke(@PathVariable UUID id, RedirectAttributes ra) {
        deviceRepository.findById(id).ifPresent(d -> {
            d.setStatus(DeviceStatus.REVOKED);
            deviceRepository.save(d);
        });
        ra.addFlashAttribute("warning", "Device revoked.");
        return "redirect:/devices";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable UUID id, RedirectAttributes ra) {
        deviceRepository.findById(id).ifPresent(d -> {
            d.setStatus(DeviceStatus.ACTIVE);
            deviceRepository.save(d);
        });
        ra.addFlashAttribute("success", "Device activated.");
        return "redirect:/devices";
    }
}
