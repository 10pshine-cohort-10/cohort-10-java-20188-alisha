/**
 * Contact Management System - Enterprise Application Entry
 */

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Navbar } from './components/Navbar';
import { ContactList } from './components/ContactList';
import { ContactFormModal } from './components/ContactFormModal';
import { ContactDetailModal } from './components/ContactDetailModal';
import { DeleteConfirmModal } from './components/DeleteConfirmModal';
import { UserProfileView } from './components/UserProfileView';
import { ChangePasswordModal } from './components/ChangePasswordModal';
import { AuthModal } from './components/AuthModal';
import { ToastProvider, useToast } from './components/Toast';
import { storage, ContactsResponse } from './services/storage';
import { Contact, User, SortConfig, FilterConfig } from './types';
import { slf4j } from './services/logger';
import { backendApi, getAuthToken, isBackendApiEnabled, setAuthToken } from './services/api';

const EMPTY_CONTACTS_RESPONSE: ContactsResponse = {
  contacts: [],
  pagination: { page: 1, pageSize: 10, totalItems: 0, totalPages: 1 },
  totalFiltered: 0,
  totalAll: 0,
  totalFavorites: 0,
};

function ContactAppContent() {
  const { showToast } = useToast();

  // Active user session
  const [currentUser, setCurrentUser] = useState<User | null>(() =>
    isBackendApiEnabled() ? null : storage.getCurrentUser()
  );
  const [activeView, setActiveView] = useState<'contacts' | 'profile'>('contacts');

  // Contact list state & parameters
  const [dataResponse, setDataResponse] = useState<ContactsResponse>(() =>
    isBackendApiEnabled() ? EMPTY_CONTACTS_RESPONSE : storage.getContacts(
      { searchQuery: '', category: 'All', isFavoriteOnly: false },
      { key: 'name', direction: 'asc' },
      1,
      10
    )
  );

  const [sortConfig, setSortConfig] = useState<SortConfig>({ key: 'name', direction: 'asc' });
  const [filterConfig, setFilterConfig] = useState<FilterConfig>({
    searchQuery: '',
    category: 'All',
    isFavoriteOnly: false,
  });
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);

  // Modals state
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
  const [isChangePasswordOpen, setIsChangePasswordOpen] = useState(false);
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);

  // Active target contact for modals
  const [targetContact, setTargetContact] = useState<Contact | null>(null);
  const [bulkDeleteIds, setBulkDeleteIds] = useState<string[]>([]);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load and refresh contacts
  const refreshContacts = useCallback(async () => {
    if (isBackendApiEnabled()) {
      if (!getAuthToken()) {
        setDataResponse(EMPTY_CONTACTS_RESPONSE);
        return;
      }
      try {
        const response = await backendApi.listContacts({
          search: filterConfig.searchQuery,
          category: filterConfig.category,
          favoriteOnly: filterConfig.isFavoriteOnly,
          page,
          size: pageSize,
          sortBy: sortConfig.key === 'name' ? 'lastName' : String(sortConfig.key),
          sortDir: sortConfig.direction,
        });
        setDataResponse({
          contacts: response.contacts,
          pagination: {
            page: response.page,
            pageSize: response.pageSize,
            totalItems: response.totalItems,
            totalPages: response.totalPages,
          },
          totalFiltered: response.totalItems,
          totalAll: response.totalAll,
          totalFavorites: response.totalFavorites,
        });
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Unable to load contacts.';
        showToast('error', 'Contacts Unavailable', message);
      }
      return;
    }
    try {
      setDataResponse(storage.getContacts(filterConfig, sortConfig, page, pageSize));
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Unable to load contacts.';
      showToast('error', 'Contacts Unavailable', message);
    }
  }, [filterConfig, sortConfig, page, pageSize, showToast]);

  useEffect(() => {
    void refreshContacts();
  }, [refreshContacts]);

  useEffect(() => {
    if (!isBackendApiEnabled() || !getAuthToken()) return;
    let active = true;
    backendApi.getProfile()
      .then((user) => {
        if (active) setCurrentUser(user);
      })
      .catch((error: unknown) => {
        if (!active) return;
        setAuthToken(null);
        setCurrentUser(null);
        const message = error instanceof Error ? error.message : 'Session expired. Please sign in again.';
        showToast('error', 'Session Expired', message);
      });
    return () => {
      active = false;
    };
  }, [showToast]);

  // Handle Sort Change
  const handleSortChange = (key: SortConfig['key']) => {
    setSortConfig((prev) => {
      if (prev.key === key) {
        return { key, direction: prev.direction === 'asc' ? 'desc' : 'asc' };
      }
      return { key, direction: 'asc' };
    });
  };

  // Handle Filter Change
  const handleFilterChange = (updates: Partial<FilterConfig>) => {
    setFilterConfig((prev) => ({ ...prev, ...updates }));
    setPage(1); // Reset to page 1 on filter change
  };

  // Modal 2: Create Contact Handler
  const handleCreateContact = async (data: Omit<Contact, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) => {
    try {
      const created = isBackendApiEnabled() ? await backendApi.createContact(data) : storage.createContact(data);
      await refreshContacts();
      setIsCreateModalOpen(false);
      showToast(
        'success',
        'Contact Created',
        `${created.firstName} ${created.lastName} has been added to the directory.`
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to save contact.';
      showToast('error', 'Error', message);
    }
  };

  // Modal 1: Update Contact Handler
  const handleUpdateContact = async (data: Omit<Contact, 'id' | 'userId' | 'createdAt' | 'updatedAt'>) => {
    if (!targetContact) return;
    try {
      const updated = isBackendApiEnabled()
        ? await backendApi.updateContact(targetContact.id, data)
        : storage.updateContact(targetContact.id, data);
      await refreshContacts();
      setIsEditModalOpen(false);
      setTargetContact(updated);
      showToast(
        'success',
        'Contact Updated',
        `Details for ${updated.firstName} ${updated.lastName} saved successfully.`
      );
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update contact.';
      showToast('error', 'Error', message);
    }
  };

  // Modal 3: Delete Contact Confirm Handler (supports single & bulk)
  const handleConfirmDelete = () => {
    setIsDeleting(true);
    setTimeout(() => {
      void (async () => {
        try {
          if (bulkDeleteIds.length > 0) {
            const count = isBackendApiEnabled()
              ? (await backendApi.bulkDelete(bulkDeleteIds)).deletedCount
              : storage.bulkDelete(bulkDeleteIds);
            setBulkDeleteIds([]);
            setIsDeleteModalOpen(false);
            await refreshContacts();
            showToast('success', 'Contacts Deleted', `Successfully purged ${count} contacts from database.`);
          } else if (targetContact) {
            const name = `${targetContact.firstName} ${targetContact.lastName}`;
            if (isBackendApiEnabled()) await backendApi.deleteContact(targetContact.id);
            else storage.deleteContact(targetContact.id);
            setIsDeleteModalOpen(false);
            setIsDetailModalOpen(false);
            setTargetContact(null);
            await refreshContacts();
            showToast('success', 'Contact Deleted', `${name} has been removed.`);
          }
        } catch (error) {
          const message = error instanceof Error
            ? error.message
            : 'Could not confirm the delete result. Refresh the directory before retrying.';
          showToast('error', 'Delete Status Unconfirmed', message);
        } finally {
          setIsDeleting(false);
        }
      })();
    }, 200);
  };

  // Toggle Favorite
  const handleToggleFavorite = async (id: string) => {
    try {
      const isFav = isBackendApiEnabled()
        ? (await backendApi.toggleFavorite(id)).isFavorite
        : storage.toggleFavorite(id);
      await refreshContacts();
      if (targetContact && targetContact.id === id) {
        setTargetContact((prev) => (prev ? { ...prev, isFavorite: isFav } : null));
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Could not update favorite status.';
      showToast('error', 'Favorite Update Failed', message);
    }
  };

  // Auth Operations
  const handleLogout = () => {
    if (isBackendApiEnabled()) setAuthToken(null);
    else storage.logout();
    setCurrentUser(null);
    setIsAuthModalOpen(true);
    showToast('info', 'Signed Out', 'Your active user session has ended.');
  };

  const handleAuthSuccess = (user: User) => {
    setCurrentUser(user);
    setIsAuthModalOpen(false);
    setActiveView('contacts');
    void refreshContacts();
  };

  // If no user is logged in, show AuthModal
  if (!currentUser) {
    return (
      <div className="min-h-screen bg-slate-900 flex flex-col justify-center items-center p-4">
        <AuthModal
          isOpen={true}
          onSuccess={handleAuthSuccess}
          defaultTab="login"
        />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f8fafc] text-slate-900 flex flex-col">
      {/* Top Navbar */}
      <Navbar
        currentUser={currentUser}
        activeView={activeView}
        onNavigate={(view) => setActiveView(view)}
        onOpenCreateModal={() => setIsCreateModalOpen(true)}
        onLogout={handleLogout}
        searchQuery={filterConfig.searchQuery}
        onSearchChange={(q) => handleFilterChange({ searchQuery: q })}
      />

      {/* Main Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {activeView === 'contacts' ? (
          <div>
            {/* View Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-5">
              <div>
                <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900">
                  Directory & Contacts
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
                  Manage enterprise contacts, multiple communication channels, and organizational profiles.
                </p>
              </div>
            </div>

            {/* Contact Listing */}
            <ContactList
              contacts={dataResponse.contacts}
              pagination={dataResponse.pagination}
              totalFiltered={dataResponse.totalFiltered}
              totalAll={dataResponse.totalAll}
              totalFavorites={dataResponse.totalFavorites}
              sortConfig={sortConfig}
              filterConfig={filterConfig}
              onSortChange={handleSortChange}
              onFilterChange={handleFilterChange}
              onPageChange={(p) => setPage(p)}
              onPageSizeChange={(ps) => {
                setPageSize(ps);
                setPage(1);
              }}
              onOpenCreateModal={() => setIsCreateModalOpen(true)}
              onOpenEditModal={(c) => {
                setTargetContact(c);
                setIsEditModalOpen(true);
              }}
              onOpenDeleteModal={(c) => {
                setTargetContact(c);
                setBulkDeleteIds([]);
                setIsDeleteModalOpen(true);
              }}
              onOpenBulkDeleteModal={(ids) => {
                setBulkDeleteIds(ids);
                setTargetContact(null);
                setIsDeleteModalOpen(true);
              }}
              onViewContactDetail={(c) => {
                setTargetContact(c);
                setIsDetailModalOpen(true);
              }}
              onToggleFavorite={handleToggleFavorite}
              onRefresh={refreshContacts}
            />
          </div>
        ) : (
          <UserProfileView
            user={currentUser}
            onLogout={handleLogout}
            onOpenChangePassword={() => setIsChangePasswordOpen(true)}
            onProfileUpdated={(up) => setCurrentUser(up)}
            totalContacts={dataResponse.totalAll}
            totalFavorites={dataResponse.totalFavorites}
          />
        )}
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-4 mt-auto">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-slate-500">
          <div>
            <span className="font-medium text-slate-700">ContactHub</span>
            <span className="text-slate-400"> — Enterprise Contact Management System</span>
          </div>
          <div>
            <span>© {new Date().getFullYear()} All rights reserved.</span>
          </div>
        </div>
      </footer>

      {/* Modal 2: Create Contact Modal */}
      <ContactFormModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSave={handleCreateContact}
        mode="create"
      />

      {/* Modal 1: Update Contact Modal */}
      <ContactFormModal
        isOpen={isEditModalOpen}
        onClose={() => {
          setIsEditModalOpen(false);
          setTargetContact(null);
        }}
        onSave={handleUpdateContact}
        initialContact={targetContact}
        mode="edit"
      />

      {/* Modal 3: Delete Contact Confirmation Modal */}
      <DeleteConfirmModal
        isOpen={isDeleteModalOpen}
        onClose={() => {
          setIsDeleteModalOpen(false);
          setTargetContact(null);
          setBulkDeleteIds([]);
        }}
        onConfirm={handleConfirmDelete}
        contact={targetContact}
        bulkCount={bulkDeleteIds.length}
        isDeleting={isDeleting}
      />

      {/* Detailed Contact Profile Modal */}
      <ContactDetailModal
        isOpen={isDetailModalOpen}
        onClose={() => {
          setIsDetailModalOpen(false);
          setTargetContact(null);
        }}
        contact={targetContact}
        onEdit={(c) => {
          setIsDetailModalOpen(false);
          setTargetContact(c);
          setIsEditModalOpen(true);
        }}
        onDelete={(c) => {
          setIsDetailModalOpen(false);
          setTargetContact(c);
          setBulkDeleteIds([]);
          setIsDeleteModalOpen(true);
        }}
        onToggleFavorite={handleToggleFavorite}
      />

      {/* Modal 4: Change Password Modal */}
      <ChangePasswordModal
        isOpen={isChangePasswordOpen}
        onClose={() => setIsChangePasswordOpen(false)}
      />
    </div>
  );
}

export default function App() {
  return (
    <ToastProvider>
      <ContactAppContent />
    </ToastProvider>
  );
}
