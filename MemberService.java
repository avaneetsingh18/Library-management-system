package com.library.service;

import com.library.model.Member;
import com.library.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MemberService - Service layer for Member operations.
 *
 * @author Avaneet Singh
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public Member addMember(Member member) {
        if (memberRepository.existsByMemberIdIgnoreCase(member.getMemberId())) {
            throw new IllegalArgumentException("Member ID '" + member.getMemberId() + "' already exists.");
        }
        if (memberRepository.existsByEmailIgnoreCase(member.getEmail())) {
            throw new IllegalArgumentException("Email '" + member.getEmail() + "' is already registered.");
        }
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Member> searchMembers(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAllMembers();
        return memberRepository.searchMembers(keyword.trim());
    }

    public Member updateMember(Long id, Member updated) {
        Member existing = getMemberById(id);

        // Validate email uniqueness if changed
        if (!existing.getEmail().equalsIgnoreCase(updated.getEmail()) &&
            memberRepository.existsByEmailIgnoreCase(updated.getEmail())) {
            throw new IllegalArgumentException("Email '" + updated.getEmail() + "' is already in use.");
        }

        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        return memberRepository.save(existing);
    }

    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long getTotalMemberCount() {
        return memberRepository.count();
    }
}
