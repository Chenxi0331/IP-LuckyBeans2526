package com.example.mentalhealth.service;

import com.example.mentalhealth.model.CounsellorNote;
import com.example.mentalhealth.model.User;
import com.example.mentalhealth.repository.CounsellorNoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounsellorNoteService {
    @Autowired
    private CounsellorNoteRepository counsellorNoteRepository;

    public List<CounsellorNote> getNotesForStudent(User student) {
        return counsellorNoteRepository.findByStudentOrderByCreatedAtDesc(student);
    }

    @SuppressWarnings("null")
    public CounsellorNote saveNote(CounsellorNote note) {
        return counsellorNoteRepository.save(note);
    }
}
