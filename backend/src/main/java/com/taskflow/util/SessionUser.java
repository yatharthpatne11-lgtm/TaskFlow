package com.taskflow.util;

import com.taskflow.dao.UserDAO;
import com.taskflow.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Returns the logged-in user with role and status re-read from the database on every call.
 * That way an admin's role change or account suspension takes effect immediately,
 * not only the next time the person logs in.
 */
public final class SessionUser {

    private static final UserDAO userDAO = new UserDAO();

    private SessionUser() {}

    /** @return the current ACTIVE user, or null if not logged in / account removed or not active. */
    public static User current(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        User cached = (session != null) ? (User) session.getAttribute("user") : null;
        if (cached == null || cached.getId() == null) {
            return null;
        }
        try {
            Optional<User> fresh = userDAO.findById(cached.getId());
            if (!fresh.isPresent() || !"ACTIVE".equalsIgnoreCase(fresh.get().getStatus())) {
                session.invalidate(); // account deleted or suspended: end the session
                return null;
            }
            session.setAttribute("user", fresh.get());
            return fresh.get();
        } catch (RuntimeException e) {
            // Database hiccup: fall back to the cached user; the action itself will report the real DB error.
            return cached;
        }
    }
}